package cn.qingke.timetable;

import java.net.URI;
import java.util.Locale;

/** Pure Java so navigation restrictions can be verified without a device. */
public final class UrlPolicy {
    public enum Navigation { ALLOW, UPGRADE_HTTPS, BLOCK }
    public static final String APP_HOST = "appassets.androidplatform.net";
    public static final String APP_URL = "https://" + APP_HOST + "/assets/www/index.html";
    public static final String COURSE_URL = "https://jwctsp.kust.edu.cn/integration/for-std/course-table";
    public static final String LOGIN_URL = "https://cas.kmust.edu.cn/lyuapServer/login";
    private static URI parse(String url) { try { return new URI(url); } catch (Exception e) { return null; } }
    private static boolean https(URI u) { return u != null && "https".equalsIgnoreCase(u.getScheme()) && u.getUserInfo() == null && (u.getPort() == -1 || u.getPort() == 443); }
    // Android/Chromium permits characters in query strings and SPA fragments
    // that java.net.URI rejects. Validate the origin independently of them.
    private static URI origin(String url){
        if(url==null)return null;
        for(int i=0;i<url.length();i++)if(url.charAt(i)<32||url.charAt(i)==127)return null;
        int end=authorityEnd(url);if(end<0)return null;
        String prefix=url.substring(0,end);if(prefix.indexOf('\\')>=0)return null;
        URI u=parse(prefix+"/");return u!=null&&u.getHost()!=null&&u.getUserInfo()==null?u:null;
    }
    private static int authorityEnd(String url){if(url==null)return -1;int start=url.indexOf("://");if(start<1)return -1;for(int i=start+3;i<url.length();i++)if("/?#".indexOf(url.charAt(i))>=0)return i;return url.length();}
    private static boolean schoolHost(URI u){
        if(u==null||u.getHost()==null)return false;
        String h=u.getHost().toLowerCase(Locale.ROOT);
        return h.equals("kmust.edu.cn")||h.endsWith(".kmust.edu.cn")||h.equals("kust.edu.cn")||h.endsWith(".kust.edu.cn");
    }
    public static boolean isLocal(String url) {
        URI u = parse(url);
        return https(u) && APP_HOST.equalsIgnoreCase(u.getHost()) && u.getPath() != null && u.getPath().startsWith("/assets/www/");
    }
    public static boolean isSchool(String url) {
        URI u=origin(url);return https(u)&&schoolHost(u);
    }
    public static boolean isCourse(String url) {
        URI u=origin(url);if(!isSchool(url)||!"jwctsp.kust.edu.cn".equalsIgnoreCase(u.getHost()))return false;
        String path=url.substring(authorityEnd(url)).split("[?#]",2)[0];
        return "/integration/for-std/course-table".equals(path)||path.startsWith("/integration/for-std/course-table/");
    }
    public static String schoolHttpsUpgrade(String url){
        URI u=origin(url);if(u==null||!"http".equalsIgnoreCase(u.getScheme())||!schoolHost(u)||(u.getPort()!=-1&&u.getPort()!=80))return null;
        String tail=url.substring(authorityEnd(url));if(tail.isEmpty()||tail.startsWith("?")||tail.startsWith("#"))tail="/"+tail;
        return "https://"+u.getHost()+tail;
    }
    public static Navigation navigation(String current,String target,boolean mainFrame,String method,boolean trustedPopup){
        if(isSchool(target))return Navigation.ALLOW;
        boolean trusted=isSchool(current)||trustedPopup&&(current==null||"about:blank".equalsIgnoreCase(current));
        if(trusted&&("about:blank".equalsIgnoreCase(target)||!mainFrame&&"about:srcdoc".equalsIgnoreCase(target)||target!=null&&target.toLowerCase(Locale.ROOT).startsWith("javascript:")))return Navigation.ALLOW;
        // Captcha/SSO subframes use normal WebView isolation and mixed-content
        // enforcement. They have no native interface and cannot become the app UI.
        if(!mainFrame&&https(origin(target)))return Navigation.ALLOW;
        if(mainFrame&&"GET".equalsIgnoreCase(method)&&schoolHttpsUpgrade(target)!=null)return Navigation.UPGRADE_HTTPS;
        return Navigation.BLOCK;
    }
    public static String diagnosticOrigin(String url){
        URI u=origin(url);if(u!=null)return u.getScheme().toLowerCase(Locale.ROOT)+"://"+u.getHost().toLowerCase(Locale.ROOT)+(u.getPort()==-1?"":":"+u.getPort());
        if(url!=null&&url.matches("(?s)^[A-Za-z][A-Za-z0-9+.-]*:.*"))return url.substring(0,url.indexOf(':')+1).toLowerCase(Locale.ROOT);
        return "无效地址";
    }
    private UrlPolicy() {}
}
