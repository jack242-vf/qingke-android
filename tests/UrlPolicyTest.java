import cn.qingke.timetable.UrlPolicy;
public class UrlPolicyTest {
    private static int count;
    private static void check(boolean test){count++;if(!test)throw new AssertionError("URL assertion "+count);}
    public static void main(String[] args){
        check(UrlPolicy.isLocal(UrlPolicy.APP_URL));
        check(!UrlPolicy.isLocal("https://appassets.androidplatform.net.attacker.test/assets/www/index.html"));
        check(!UrlPolicy.isLocal("https://appassets.androidplatform.net@attacker.test/assets/www/index.html"));
        check(!UrlPolicy.isLocal("http://appassets.androidplatform.net/assets/www/index.html"));
        check(!UrlPolicy.isLocal("https://appassets.androidplatform.net:444/assets/www/index.html"));
        check(UrlPolicy.isSchool(UrlPolicy.LOGIN_URL));check(UrlPolicy.isSchool(UrlPolicy.COURSE_URL));
        check(!UrlPolicy.isSchool("https://fakekmust.edu.cn/login"));check(!UrlPolicy.isSchool("https://cas.kmust.edu.cn.attacker.test/login"));
        check(!UrlPolicy.isSchool("javascript:alert(1)"));check(!UrlPolicy.isSchool("intent://example"));
        check(UrlPolicy.isCourse(UrlPolicy.COURSE_URL+"/info/all-courses?semesterId=381"));
        check(!UrlPolicy.isCourse("https://i.kust.edu.cn/#/index1"));
        check(!UrlPolicy.isCourse(UrlPolicy.COURSE_URL+"-other"));
        String school=UrlPolicy.LOGIN_URL;
        check(UrlPolicy.isSchool("https://i.kust.edu.cn/#/index1?data={\"section\":\"课表\"}"));
        check(UrlPolicy.isCourse(UrlPolicy.COURSE_URL+"?data={\"semester\":381}"));
        check(!UrlPolicy.isSchool("https://i.kust.edu.cn.attacker.test/#/{\"section\":\"课表\"}"));
        check(!UrlPolicy.isSchool("https://cas.kmust.edu.cn@attacker.test/login"));
        check(!UrlPolicy.isSchool("https://cas.kmust.edu.cn\\@attacker.test/login"));
        check(!UrlPolicy.isSchool("https://cas.kmust.edu.cn%2e.attacker.test/login"));
        check(!UrlPolicy.isSchool("https://cas.kmust.edu.cn:8443/login"));
        check(UrlPolicy.navigation(school,"about:blank",true,"GET",false)==UrlPolicy.Navigation.ALLOW);
        check(UrlPolicy.navigation(null,"about:blank",true,"GET",true)==UrlPolicy.Navigation.ALLOW);
        check(UrlPolicy.navigation(null,"about:blank",true,"GET",false)==UrlPolicy.Navigation.BLOCK);
        check(UrlPolicy.navigation(school,"javascript:login()",true,"GET",false)==UrlPolicy.Navigation.ALLOW);
        check(UrlPolicy.navigation("https://attacker.test/","javascript:login()",true,"GET",false)==UrlPolicy.Navigation.BLOCK);
        check(UrlPolicy.navigation(school,"about:srcdoc",false,"GET",false)==UrlPolicy.Navigation.ALLOW);
        check(UrlPolicy.navigation(school,"https://captcha.example.test/frame",false,"GET",false)==UrlPolicy.Navigation.ALLOW);
        check(UrlPolicy.navigation(school,"https://captcha.example.test/frame",true,"GET",false)==UrlPolicy.Navigation.BLOCK);
        check(UrlPolicy.navigation(school,"http://i.kust.edu.cn/#/index1",true,"GET",false)==UrlPolicy.Navigation.UPGRADE_HTTPS);
        check(UrlPolicy.navigation(school,"http://i.kust.edu.cn/login",true,"POST",false)==UrlPolicy.Navigation.BLOCK);
        check(UrlPolicy.navigation(school,"http://i.kust.edu.cn/frame",false,"GET",false)==UrlPolicy.Navigation.BLOCK);
        check(UrlPolicy.navigation(school,"http://attacker.test/",true,"GET",false)==UrlPolicy.Navigation.BLOCK);
        check("https://i.kust.edu.cn/#/index1?data={\"x\":1}".equals(UrlPolicy.schoolHttpsUpgrade("http://i.kust.edu.cn:80/#/index1?data={\"x\":1}")));
        check(UrlPolicy.schoolHttpsUpgrade("http://i.kust.edu.cn:8080/login")==null);
        check(UrlPolicy.schoolHttpsUpgrade("http://cas.kmust.edu.cn@attacker.test/")==null);
        check(UrlPolicy.schoolHttpsUpgrade("https://i.kust.edu.cn/")==null);
        check("https://cas.kmust.edu.cn".equals(UrlPolicy.diagnosticOrigin(school+"?username=private&ticket=private#password=private")));
        check("javascript:".equals(UrlPolicy.diagnosticOrigin("javascript:login('private')")));
        check(!UrlPolicy.isLocal(UrlPolicy.LOGIN_URL));
        System.out.println(count+" Android navigation-policy assertions passed");
    }
}
