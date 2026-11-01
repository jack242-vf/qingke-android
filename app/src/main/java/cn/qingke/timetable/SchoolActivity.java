package cn.qingke.timetable;

import android.app.*;
import android.content.Intent;
import android.content.ClipData;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Login stays in this WebView; only user-triggered course-table DOM is returned. */
public class SchoolActivity extends Activity {
    private WebView school;private TextView status;private Button extractButton;
    private FrameLayout pages;
    private final List<WebView> windows=new ArrayList<>();
    private String lastBlocked="无",lastReason="无";
    private boolean extracting,pendingExtraction;
    private int attempts,generation;
    private final android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable retry=()->attemptExtract(generation);
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(0xfff6f8f4);
        LinearLayout bar=new LinearLayout(this);bar.setPadding(8,4,8,4);
        addButton(bar,"返回",()->finish());addButton(bar,"课表入口",()->{cancelExtraction();school.loadUrl(UrlPolicy.COURSE_URL);});extractButton=addButton(bar,"提取课表",this::extract);
        root.addView(bar);
        status=new TextView(this);status.setText("在学校页面自行登录 → 课表入口 → 提取课表。密码不交给轻课。");status.setPadding(16,10,16,12);status.setTextColor(0xff247565);root.addView(status);
        status.setText("在学校页面自行登录，再点「课表入口」。点击此提示可查看帮助。");
        status.setTextSize(12);status.setOnClickListener(v->showHelp());
        pages=new FrameLayout(this);root.addView(pages,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
        school=createPage(false);activate(school);
        if(state==null||school.restoreState(state)==null)school.loadUrl(UrlPolicy.LOGIN_URL);
    }
    private WebView createPage(boolean popup){
        WebView page=new WebView(this);WebSupport.configure(page);
        page.getSettings().setSupportMultipleWindows(true);page.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(page,true);
        final WebChromeClient normal=WebSupport.dialogs(this);
        page.setWebChromeClient(new WebChromeClient(){
            @Override public boolean onJsAlert(WebView v,String u,String m,JsResult r){return normal.onJsAlert(v,u,m,r);}
            @Override public boolean onJsConfirm(WebView v,String u,String m,JsResult r){return normal.onJsConfirm(v,u,m,r);}
            @Override public boolean onJsPrompt(WebView v,String u,String m,String value,JsPromptResult r){return normal.onJsPrompt(v,u,m,value,r);}
            @Override public boolean onCreateWindow(WebView opener,boolean dialog,boolean gesture,android.os.Message message){
                if(!UrlPolicy.isSchool(opener.getUrl())||windows.size()>=5){status.setText("无法打开更多登录窗口，请点此查看帮助。");return false;}
                // Keep the child WebView, form POSTs, and window.opener callbacks.
                WebView child=createPage(true);activate(child);status.setText("正在打开学校窗口…");
                ((WebView.WebViewTransport)message.obj).setWebView(child);message.sendToTarget();return true;
            }
            @Override public void onCloseWindow(WebView window){closePage(window);}
        });
        page.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest request){return handleNavigation(v,request,popup);}
            @Override public void onPageFinished(WebView v,String url){
                CookieManager.getInstance().flush();if(v!=school)return;
                if(pendingExtraction){extracting=false;scheduleRetry();}
                else if(UrlPolicy.isCourse(url))status.setText("已进入课表。选好学期，点一次「提取课表」即可。");
                else if(UrlPolicy.isSchool(url))status.setText("请在学校页面完成登录，再点「课表入口」。点击此提示可查看帮助。");
            }
            @Override public void onReceivedSslError(WebView v,android.webkit.SslErrorHandler ssl,android.net.http.SslError error){ssl.cancel();cancelExtraction();lastBlocked=UrlPolicy.diagnosticOrigin(error.getUrl());lastReason="证书验证失败";status.setText("学校页面证书验证失败，已停止加载。点此查看帮助。");}
            @Override public void onReceivedError(WebView v,WebResourceRequest request,WebResourceError error){if(request.isForMainFrame()&&v==school){cancelExtraction();lastBlocked=UrlPolicy.diagnosticOrigin(request.getUrl().toString());lastReason="加载错误 "+error.getErrorCode();status.setText("页面暂时无法打开（"+lastBlocked+"）。可重试课表入口，或点此查看帮助。");}}
        });
        windows.add(page);pages.addView(page,new FrameLayout.LayoutParams(-1,-1));return page;
    }
    private boolean handleNavigation(WebView view,WebResourceRequest request,boolean popup){
        String target=request.getUrl().toString();
        UrlPolicy.Navigation action=UrlPolicy.navigation(view.getUrl(),target,request.isForMainFrame(),request.getMethod(),popup);
        if(action==UrlPolicy.Navigation.ALLOW)return false;
        if(action==UrlPolicy.Navigation.UPGRADE_HTTPS){
            if(request.isForMainFrame()&&view==school)status.setText("学校旧地址正在改用 HTTPS 继续登录…");
            view.loadUrl(UrlPolicy.schoolHttpsUpgrade(target));return true;
        }
        if(request.isForMainFrame()&&view==school){lastBlocked=UrlPolicy.diagnosticOrigin(target);lastReason="主页面跳转未允许";status.setText("未能继续跳转："+lastBlocked+"。点击此提示查看帮助。");}
        return true;
    }
    private void activate(WebView page){school=page;for(WebView v:windows)v.setVisibility(v==page?android.view.View.VISIBLE:android.view.View.GONE);}
    private void closePage(WebView page){
        if(windows.size()<=1||!windows.contains(page))return;
        boolean active=page==school;windows.remove(page);pages.removeView(page);page.destroy();
        if(active){activate(windows.get(windows.size()-1));if(pendingExtraction){extracting=false;scheduleRetry();}else status.setText("学校窗口已关闭，可点击课表入口继续。");}
    }
    private void showHelp(){
        String version="0.4.1",providerName="未知";
        try{version=getPackageManager().getPackageInfo(getPackageName(),0).versionName;android.content.pm.PackageInfo provider=WebView.getCurrentWebViewPackage();if(provider!=null)providerName=provider.packageName+" "+provider.versionName;}catch(Exception ignored){}
        final String diagnostics="轻课 "+version+"\nAndroid API: "+android.os.Build.VERSION.SDK_INT+"\nWebView: "+providerName+"\n当前域名: "+UrlPolicy.diagnosticOrigin(school.getUrl())+"\n最近异常域名: "+lastBlocked+"\n异常类型: "+lastReason+"\n打开窗口数: "+windows.size();
        new AlertDialog.Builder(this).setTitle("登录帮助").setMessage("先在学校页面完成登录，再点击课表入口。\n\n若仍无法进入，可复制下面的排查信息，不包含账号、密码、验证码、Cookie 或网址参数。\n\n"+diagnostics)
            .setPositiveButton("复制排查信息",(d,w)->{android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);clipboard.setPrimaryClip(ClipData.newPlainText("轻课登录排查",diagnostics));Toast.makeText(this,"已复制排查信息",Toast.LENGTH_SHORT).show();})
            .setNeutralButton("重开登录入口",(d,w)->{cancelExtraction();school.loadUrl(UrlPolicy.LOGIN_URL);}).setNegativeButton("关闭",null).show();
    }
    private Button addButton(LinearLayout bar,String title,Runnable action){Button b=new Button(this);b.setText(title);b.setTextColor(0xff247565);bar.addView(b,new LinearLayout.LayoutParams(0,-2,1));b.setOnClickListener(v->action.run());return b;}
    private void extract(){
        if(pendingExtraction)return;
        if(!UrlPolicy.isCourse(school.getUrl())){status.setText("请先完成学校登录，再点「课表入口」。");return;}
        pendingExtraction=true;attempts=0;generation++;extractButton.setEnabled(false);extractButton.setText("读取中…");
        status.setText("正在读取课表，请保持此页面打开…");attemptExtract(generation);
    }
    private void scheduleRetry(){handler.removeCallbacks(retry);if(pendingExtraction)handler.postDelayed(retry,750);}
    private void cancelExtraction(){pendingExtraction=false;extracting=false;generation++;handler.removeCallbacks(retry);if(extractButton!=null){extractButton.setEnabled(true);extractButton.setText("提取课表");}}
    private void attemptExtract(int token){
        if(!pendingExtraction||extracting||isFinishing()||isDestroyed()||token!=generation)return;
        if(++attempts>40){cancelExtraction();status.setText("等待课程列表超时。请检查网络或确认所选学期有课，页面加载完成后可重试。");return;}
        String url=school.getUrl();if(url==null||"about:blank".equalsIgnoreCase(url)){scheduleRetry();return;}
        if(!UrlPolicy.isCourse(url)){cancelExtraction();status.setText("登录状态已变化，请完成登录，再点击课表入口重试。");return;}
        try{
            String script=readAsset("native-extract.js");extracting=true;
            school.evaluateJavascript(script,value->{
                if(token!=generation||isFinishing()||isDestroyed())return;
                extracting=false;
                try{
                    if(value==null||value.length()>400000)throw new Exception("课程数据过大");
                    JSONObject obj=new JSONObject(value);
                    if(obj.optBoolean("pending")){status.setText(obj.optString("error","正在等待课表加载…"));scheduleRetry();return;}
                    if(obj.has("error")){cancelExtraction();status.setText(obj.getString("error"));return;}
                    JSONArray rows=obj.getJSONArray("rows");if(rows.length()==0||rows.length()>200)throw new Exception("课程数量不正确");
                    for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);if(row.getString("name").length()>40||row.getString("schedule").length()>30000)throw new Exception("课程格式异常");}
                    cancelExtraction();Intent result=new Intent().putExtra("payload",obj.toString());setResult(RESULT_OK,result);finish();
                }catch(Exception e){scheduleRetry();status.setText("正在等待页面就绪…（"+attempts+"/40）");}
            });
        }catch(IOException e){cancelExtraction();status.setText("提取组件加载失败，请重新安装应用。");}
    }
    private String readAsset(String name)throws IOException{try(InputStream in=getAssets().open(name);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return new String(out.toByteArray(),StandardCharsets.UTF_8);}}
    @Override protected void onSaveInstanceState(Bundle s){super.onSaveInstanceState(s);school.saveState(s);}
    @Override public void onBackPressed(){if(school.canGoBack())school.goBack();else if(windows.size()>1)closePage(school);else super.onBackPressed();}
    @Override protected void onDestroy(){cancelExtraction();for(WebView page:windows)page.destroy();windows.clear();super.onDestroy();}
}
