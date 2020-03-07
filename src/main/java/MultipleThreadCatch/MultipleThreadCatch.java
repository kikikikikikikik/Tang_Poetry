package MultipleThreadCatch;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomText;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.NlpAnalysis;
import javax.sql.DataSource;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MultipleThreadCatch {
    private static AtomicInteger successCount = new AtomicInteger(0);
    private static AtomicInteger failureCount = new AtomicInteger(0);
    private static class Job implements Runnable {
        private String url;
        private DataSource dataSource;
        //private CountDownLatch countDownLatch;

        public Job(String url,DataSource dataSource/*,CountDownLatch countDownLatch*/) {
            this.url = url;
            this.dataSource = dataSource;
            //this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            WebClient client = new WebClient(BrowserVersion.CHROME);
            client.getOptions().setJavaScriptEnabled(false);
            client.getOptions().setCssEnabled(false);
            try {
                MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

                HtmlPage page = client.getPage(url);
                String xpath;
                DomText domText;
                xpath = "//div[@class='cont']/h1/text()";
                domText = (DomText) page.getBody().getByXPath(xpath).get(0);
                String title = domText.asText();

                xpath = "//div[@class='cont']/p[@class='source']/a[1]/text()";
                domText = (DomText) page.getBody().getByXPath(xpath).get(0);
                String dynasty = domText.asText();

                xpath = "//div[@class='cont']/p[@class='source']/a[2]/text()";
                domText = (DomText) page.getBody().getByXPath(xpath).get(0);
                String author = domText.asText();

                xpath = "//div[@class='cont']/div[@class='contson']";
                HtmlElement element = (HtmlElement) page.getBody().getByXPath(xpath).get(0);
                String content = element.getTextContent().trim();

                //1.计算sha256
                String s = title + content;
                messageDigest.update(s.getBytes("UTF-8"));
                byte[] result = messageDigest.digest();
                StringBuilder sha256 = new StringBuilder();
                for (byte b : result) {
                    sha256.append(String.format("%02x", b));
                }

                //2.计算分词
                List<Term> termList = new ArrayList<>();
                termList.addAll(NlpAnalysis.parse(title).getTerms());
                termList.addAll(NlpAnalysis.parse(content).getTerms());
                List<String> words = new ArrayList<>();
                for (Term term : termList) {
                    if (term.getNatureStr().equals("w")) {
                        continue;
                    }
                    if (term.getNatureStr().equals("null")) {
                        continue;
                    }
                    if (term.getNatureStr().length() < 2) {
                        continue;
                    }
                    words.add(term.getRealName());
                }
                String insertWords = String.join(",", words);

                try (Connection connection = dataSource.getConnection()) {
                    String sql = "insert into tangshi" +
                            " (sha256,dynasty,title,author,content,words)" +
                            "values (?,?,?,?,?,?)";
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setString(1, sha256.toString());
                        statement.setString(2, dynasty);
                        statement.setString(3, title);
                        statement.setString(4, author);
                        statement.setString(5, content);
                        statement.setString(6, insertWords);
                        statement.executeUpdate();
                        //System.out.println(title + "插入成功！");
                        successCount.getAndIncrement();
                    }
                }
            }catch (IOException e){
                e.printStackTrace();
                failureCount.getAndIncrement();
            }catch (SQLException e){
                if(!e.getMessage().contains("Duplicate entry")){
                    e.printStackTrace();
                    failureCount.getAndIncrement();
                }else {
                    successCount.getAndIncrement();
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                failureCount.getAndIncrement();
            }
           // countDownLatch.countDown();
        }
    }

        public static void main(String[] args) throws IOException, SQLException, NoSuchAlgorithmException, InterruptedException {
            ExecutorService pool = Executors.newFixedThreadPool(30);
            WebClient client = new WebClient(BrowserVersion.CHROME);
            client.getOptions().setCssEnabled(false);
            client.getOptions().setJavaScriptEnabled(false);

            String baseUrl = "https://so.gushiwen.org";
            String pathUrl = "/gushi/tangshi.aspx";

            List<String> detailUrlList = new ArrayList<>();
            //列表页的解析
            {
                String url = baseUrl + pathUrl;
                HtmlPage page = client.getPage(url);
                List<HtmlElement> divs = page.getBody().getElementsByAttribute("div", "class", "typecont");
                for (HtmlElement div : divs) {
                    List<HtmlElement> as = div.getElementsByTagName("a");
                    for (HtmlElement a : as) {
                        String detailUrl = a.getAttribute("href");
                        detailUrlList.add(baseUrl + detailUrl);
                    }
                }
            }
            MysqlConnectionPoolDataSource dataSource = new MysqlConnectionPoolDataSource();
            dataSource.setServerName("127.0.0.1");
            dataSource.setPort(3306);
            dataSource.setUser("root");
            dataSource.setPassword("1002");
            dataSource.setDatabaseName("tangshi");
            dataSource.setUseSSL(false);
            dataSource.setCharacterEncoding("UTF8");

            CountDownLatch countDownLatch = new CountDownLatch(detailUrlList.size());
            //详情页的请求和解析
            for (String url : detailUrlList) {
                pool.execute(new Job(url,dataSource/*,countDownLatch*/));
            }
            //等待所有诗都下载结束
            //countDownLatch.countDown();
            while (successCount.get()+failureCount.get() < detailUrlList.size()){
                System.out.printf("一共%d首诗，成功%d，失败%d\r",detailUrlList.size(),successCount.get(),failureCount.get());
                TimeUnit.MILLISECONDS.sleep(10);
            }
            System.out.println("全部下载成功");
            pool.shutdown();
        }
    }
