package Lab;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import javax.xml.bind.Element;
import java.io.File;
import java.io.IOException;
import java.util.List;
//列表页下载提取Demo
public class HtmlUnitDemo {
    public static void main(String[] args) throws IOException {
        //无界面的浏览器（HTTP客户端）模拟谷歌浏览器
        WebClient webClient = new WebClient(BrowserVersion.CHROME);
        //关闭了浏览器的js执行引擎，不再执行网页中的js脚本
        webClient.getOptions().setJavaScriptEnabled(false);
        //关闭了浏览器的css执行引擎，不再执行网页中的css布局
        webClient.getOptions().setCssEnabled(false);
        //访问url对应的网站
        HtmlPage page = webClient.getPage("https://so.gushiwen.org/gushi/tangshi.aspx");
        System.out.println(page);
        File file = (new File("唐诗三百首\\列表页.html"));
        //如何从html中提取我们需要的信息
        HtmlElement body = page.getBody();
        List<HtmlElement> elements = body.getElementsByAttribute("div","class","typecont");
        int count = 0;
        for(HtmlElement element:elements){
          // elementName:"div",attributeName:"class"
           List<HtmlElement> aElements = element.getElementsByTagName("a");
           for(HtmlElement a : aElements){
               System.out.println(a.getAttribute("href"));
               count++;
           }
        }
        System.out.println(count);
    }
}
