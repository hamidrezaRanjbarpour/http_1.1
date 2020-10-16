import org.jsoup.Jsoup;

import java.util.regex.Pattern;

public class Temp {

    public static void main(String[] args) {
        var str = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <link rel=\"stylesheet\" href=\"./someshit/style.css\">\n" +
                "    <link rel=\"stylesheet\" href=\"./someshit/bootstrap.css\">\n" +
                "    <link rel=\"stylesheet\" href=\"https://www.example.com\">\n" +
                "</head>\n" +
                "<body>\n" +
                "\n" +
                "<img src=\"./glsdj/fdslkj.png\" alt=\"\">\n" +
                "<img src=\"https://someshit.org\" alt=\"\">\n" +
                "<img src=\"www.gf.com\" data-src=\"https://anothershit.ico\" alt=\"\">\n" +
                "\n" +
                "<div id=\"ctl00_cphHeader_cphHeader_parent_row_131\" class=\"placeHolderWrapper\"\n" +
                "     data-background=\"https://cdn.time.ir/Content/media/image/2019/10/91_orig.jpg\"></div>\n" +
                "\n" +
                "<div id=\"sfdsdf\" class=\"placeHolderWrapper\"\n" +
                "     data-background=\"kjfdslkfdj\"></div>\n" +
                "\n" +
                "<div class=\"container\"></div>\n" +
                "\n" +
                "</body>\n" +
                "<script src=\"\"></script>\n" +
                "</html>";

        var doc = Jsoup.parse(str);

//        var elem = doc.select("link:not([href^=https://]), img");
        var something = doc.select("link, img, script, div[data-background]").select(":not([href^=https://])").select(":not([src^=https://])").select(":not(div[data-background^=https://])");
//        System.out.println(elem[1].attr("href"));
        var rel = "./dslkfjds/clkfsdj.js";
        var rel_2 = "https://something.com";

        var pattern = Pattern.compile("^((?!www\\.|(?:http|ftp)s?://|[A-Za-z]:\\\\|//).*)");

        var match1 = pattern.matcher(rel);
        var match2 = pattern.matcher(rel_2);
        if (match1.matches()) {
            System.out.println(match1.group(1));
        }

    }
}
