import org.jsoup.Jsoup;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

public class Main {
    final static int PORT = 80;
    final static String CRLF = "\r\n";
    static int htmlFileNumber = 0;
    static int jsonFileNumber = 0;
    static String hostName;

    public static void main(String[] args) {
        var URL_REGEX = Pattern.compile("(?i)(http://)?([-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b)([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)");
        var VALID_METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE");
        var scanner = new Scanner(System.in);
        var input = "";
        var studentID = "";

        System.out.println("Content-Type: text/html; charset=utf-8".split(":| \\s|;")[1].trim());
        while (true) {

            System.out.println("Enter a command or url:");
            input = scanner.nextLine();
            input = input.trim().toLowerCase();
            if (input.equals("exit")) {
                break;
            } else if (input.equals("set-student-id-header")) {
                System.out.println("Enter student id:");
                input = scanner.nextLine().trim();

                try {
                    Integer.parseInt(input);
                    studentID = input;
                } catch (NumberFormatException e) {
                    System.err.println("Invalid type! Student ID must be integer.");
                }

            } else if (input.equals("remove-student-id-header")) {
                studentID = "";
            } else {
                var matchObj = URL_REGEX.matcher(input);
                if (matchObj.matches()) {

                    System.out.println("Enter http method:");
                    System.out.println("GET for get method, \n" +
                            "POST for post method, \n" +
                            "PUT for put method, \n" +
                            "PATCH for patch method, \n" +
                            "DELETE for delete method.\n");

                    input = scanner.nextLine();
                    input = input.trim().toUpperCase();
                    if (!VALID_METHODS.contains(input)) {
                        System.err.println("Invalid http method!");
                        continue;
                    }

                    /*
                    GET HTTP VERSION HERE.
                    */
                    var method = input;
                    hostName = matchObj.group(2);
                    var resources = matchObj.group(3);

                    if (resources.isEmpty()) {
                        resources = "/";
                    }

                    try (var tcpSocket = new Socket(hostName, PORT);
                         var outStream = new OutputStreamWriter(tcpSocket.getOutputStream());
                         var inStream = tcpSocket.getInputStream()) {

                        outStream.write(makeRequestMessage(method, resources, studentID));
                        outStream.flush();

                        parseResponse(inStream, outStream);

//                        var inSc = new Scanner(inStream);
//                        var response = new StringBuilder();
//                        while (inSc.hasNextLine()) {
//                            response.append(inSc.nextLine()).append(CRLF);
//                        }

//                        System.out.println(response.toString());

                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                } else
                    System.err.println("Invalid command or url!");
            }


        }

    }

    private static void parseResponse(InputStream inStream, OutputStreamWriter outputStream, String filename) {
        var pattern = Pattern.compile("(HTTP/[12].[\\d+]) ([\\d]{3}) ([A-Za-z ]+)");
        Scanner in = new Scanner(inStream);
        var statusCode = "";
        var statusText = "";
        var response = new StringBuilder();
        var contentType = "";
        var parameter = "";
        var messageBody = new StringBuilder();
        var isBody = false;
        while (in.hasNextLine()) {
            var line = in.nextLine();
            var matchObj = pattern.matcher(line);
            if (matchObj.matches()) {
                /* Then it is status line of HTTP response. */
                var arr = line.split(" ");
                statusCode = matchObj.group(2);
                statusText = matchObj.group(3);
            }
            if (line.startsWith("Content-Type")) {
                var arr = line.split(":|;");
                contentType = arr[1].trim();
                parameter = arr[2].trim();
            }
            if (line.isEmpty())
                isBody = true;
            if (isBody && !line.isEmpty()) {
                messageBody.append(line).append(CRLF);
            } else
                response.append(line).append(CRLF);
        }

        if (contentType.equals("text/html")) {
            htmlFileNumber++;
            writeToFile(messageBody.toString(), ".html");
            downloadLinkedResources(messageBody.toString(), inStream, outputStream);

        } else if (contentType.equals("application/json")) {
            jsonFileNumber++;
            writeToFile(messageBody.toString(), ".json");
        } else if (contentType.equals("text/plain")) {
            response.append(messageBody);
        } else {
            /*
            Other MIME type formats should be downloaded.
             */


        }

        System.out.println(response);
        /* Handle different status codes */
        if (statusCode.charAt(0) == '3') {
            System.err.println("Redirect: " + statusText);
        }
        if (statusCode.charAt(0) == '4') {
            System.err.println("Client Error: " + statusText);
        }
        if (statusCode.charAt(0) == '5') {
            System.err.println("Server Error: " + statusText);
        }

    }

    private static void downloadLinkedResources(String messageBody, InputStream inputStream, OutputStreamWriter outputStream) {
//        var scanner = new Scanner(inputStream);

        var pattern = Pattern.compile("^((?!www\\.|(?:http|ftp)s?://|[A-Za-z]:\\\\|//).*)");

        var doc = Jsoup.parse(messageBody);
        var elements = doc.select("link, img, script, div[data-background]").select(":not([href^=https://])").select(":not([src^=https://])").select(":not(div[data-background^=https://])");
        for (var element : elements) {
//            if (element.tagName().equals("img") && element.hasAttr("data-src")) {
//                /* 2 HTTP REQs:
//                    1) for src=""
//                    2) for data-src=""
//                 */
//            } else {
//
//            }
            var url = "";
            var value = "";
            if (element.hasAttr("src")) {
                value = element.attr("src");
            }
            if (element.hasAttr("href")) {
                value = element.attr("href");

            }
            if (element.hasAttr("data-src")) {
                value = element.attr("data-src");

            }
            if (element.hasAttr("data-background")) {
                value = element.attr("data-background");
            }

            var matchObj = pattern.matcher(value);
            if (matchObj.matches()) {
                url = matchObj.group(1);
                StringBuilder pure_url = new StringBuilder();
                /* remove first dot and last / from url */
                if (url.startsWith("."))
                    pure_url.append(url.substring(1));
                if (url.endsWith("/"))
                    pure_url.deleteCharAt(pure_url.length() - 1);

                var requestMessage = makeRequestMessage("GET", pure_url.toString(), "");
                try {
                    outputStream.write(requestMessage);
                    outputStream.flush();

                    parseResponse(inputStream, outputStream, pure_url.toString());

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    private static void writeToFile(String messageBody, String extension) {
        var index = new StringBuilder();

        if (extension.equals(".html") && htmlFileNumber > 1)
            index.append("(").append(htmlFileNumber).append(")");
        else if (extension.equals(".json") && jsonFileNumber > 1)
            index.append("(").append(jsonFileNumber).append(")");

        var fileName = new StringBuilder();
        fileName.append("Server_Response").append(index).append(extension);
        File file = new File(fileName.toString());

        try (var fileWriter = new FileWriter(file)) {
            fileWriter.write(messageBody);
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String makeRequestMessage(String method, String path, String studentID) {
        StringBuilder message = new StringBuilder();

//        message.append(method).append(" ").append(path).append(" ").append("HTTP/1.0").append(CRLF);
        message.append(method).append(" ").append(path).append(" ").append("HTTP/1.1").append(CRLF);
        message.append("Host: ").append(hostName).append(CRLF);
        message.append("Connection: ").append("keep-alive").append(CRLF);

        message.append("Accept: ").append("*/*").append(CRLF);
        message.append("Accept-Language: ").append("*").append(CRLF);
        message.append("Accept-Encoding: ").append("gzip, deflate, br").append(CRLF);

        if (!studentID.isEmpty()) {
            message.append("x-student-id:").append(studentID).append(CRLF);
        }
        /* End of header section */
        message.append(CRLF);
        return message.toString();

        /*
        ADD ADDITIONAL HEADERS TO HTTP REQ.
         */
    }
}
