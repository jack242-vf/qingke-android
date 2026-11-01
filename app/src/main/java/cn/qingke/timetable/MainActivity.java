package cn.qingke.timetable;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.*;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int SCHOOL=100, CHOOSE_FILE=101, EXPORT_FILE=102;
    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private String pendingExport;
    private String pendingSchool;
    private boolean pageReady;
    private NativeTools nativeTools;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        nativeTools=new NativeTools(this);
        web=new WebView(this);WebSupport.configure(web);web.getSettings().setAllowContentAccess(true);setContentView(web);
        web.getSettings().setUserAgentString(web.getSettings().getUserAgentString()+" QingkeAndroid/0.4.1");
        web.setBackgroundColor(0xfff6f8f4);
        web.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r) {
                String url=r.getUrl().toString();
                if (!UrlPolicy.isLocal(url)) return response(403,"Forbidden","text/plain",new byte[0]);
                String path=r.getUrl().getPath().substring("/assets/".length());
                if(path.contains("..") || path.contains("\\"))return response(403,"Forbidden","text/plain",new byte[0]);
                try {return new WebResourceResponse(mime(path),"UTF-8",getAssets().open(path));}
                catch(IOException e){return response(404,"Not Found","text/plain",new byte[0]);}
            }
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r) {
                if(UrlPolicy.isLocal(r.getUrl().toString()))return false;
                if(r.isForMainFrame() && UrlPolicy.isSchool(r.getUrl().toString()))openSchool();
                return true;
            }
            @Override public void onPageStarted(WebView v,String url,android.graphics.Bitmap icon){pageReady=false;}
            @Override public void onPageFinished(WebView v,String url){pageReady=UrlPolicy.isLocal(url);deliverSchool();}
        });
        final WebChromeClient normal=WebSupport.dialogs(this);
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onJsAlert(WebView v,String u,String m,JsResult r){return normal.onJsAlert(v,u,m,r);}
            @Override public boolean onJsConfirm(WebView v,String u,String m,JsResult r){return normal.onJsConfirm(v,u,m,r);}
            @Override public boolean onJsPrompt(WebView v,String url,String message,String value,JsPromptResult result) {
                if(!"__qingke_native__".equals(message))return normal.onJsPrompt(v,url,message,value,result);
                if(!UrlPolicy.isLocal(url)||!UrlPolicy.isLocal(v.getUrl())||value==null||value.length()>1000000){result.cancel();return true;}
                try {
                    JSONObject cmd=new JSONObject(value);
                    if("openSchool".equals(cmd.optString("action"))) {result.confirm("ok");openSchool();}
                    else if("exportBackup".equals(cmd.optString("action"))) {
                        JSONObject backup=cmd.getJSONObject("data");
                        if((backup.getInt("version")!=1&&backup.getInt("version")!=2)||backup.getJSONArray("courses").length()>500)throw new Exception();
                        pendingExport=backup.toString(2);result.confirm("ok");
                        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json").putExtra(Intent.EXTRA_TITLE,"轻课备份.json");
                        startActivityForResult(i,EXPORT_FILE);
                    } else if("syncSchedule".equals(cmd.optString("action"))) {
                        try{ScheduleRepository.save(MainActivity.this,cmd.getJSONObject("snapshot"));result.confirm("{\"ok\":true}");}
                        catch(Exception syncError){ScheduleRepository.invalidate(MainActivity.this);result.confirm("{\"ok\":false}");}
                        web.post(()->{ReminderScheduler.rebuild(MainActivity.this);ScheduleWidget.updateAll(MainActivity.this);});
                    } else if("openNativeTools".equals(cmd.optString("action"))){result.confirm("ok");nativeTools.show();}
                    else result.cancel();
                }catch(Exception e){result.cancel();Toast.makeText(MainActivity.this,"操作失败，请重试",Toast.LENGTH_SHORT).show();}
                return true;
            }
            @Override public boolean onShowFileChooser(WebView v,ValueCallback<Uri[]> callback,FileChooserParams params) {
                if(!UrlPolicy.isLocal(v.getUrl()))return false;
                if(fileCallback!=null)fileCallback.onReceiveValue(null);fileCallback=callback;
                try {startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json"),CHOOSE_FILE);}
                catch(ActivityNotFoundException e){fileCallback.onReceiveValue(null);fileCallback=null;}
                return true;
            }
        });
        if(state!=null){pendingSchool=state.getString("pendingSchool");pendingExport=state.getString("pendingExport");}
        web.loadUrl(UrlPolicy.APP_URL);
    }
    private void openSchool(){startActivityForResult(new Intent(this,SchoolActivity.class),SCHOOL);}
    @Override protected void onResume(){super.onResume();if(nativeTools!=null)nativeTools.resume();}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==NativeTools.NOTIFICATION_PERMISSION&&nativeTools!=null)nativeTools.permissionResult();}
    private void deliverSchool(){
        if(!pageReady||pendingSchool==null||!UrlPolicy.isLocal(web.getUrl()))return;
        String value=pendingSchool;pendingSchool=null;
        web.evaluateJavascript("window.dispatchEvent(new CustomEvent('qingke-school-import',{detail:JSON.parse("+JSONObject.quote(value)+")}))",null);
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==SCHOOL&&result==RESULT_OK&&data!=null){pendingSchool=data.getStringExtra("payload");deliverSchool();}
        else if(request==CHOOSE_FILE&&fileCallback!=null){fileCallback.onReceiveValue(result==RESULT_OK&&data!=null&&data.getData()!=null?new Uri[]{data.getData()}:null);fileCallback=null;}
        else if(request==EXPORT_FILE){
            if(result==RESULT_OK&&data!=null&&data.getData()!=null&&pendingExport!=null){
                try(OutputStream out=getContentResolver().openOutputStream(data.getData())){if(out==null)throw new IOException();out.write(pendingExport.getBytes(StandardCharsets.UTF_8));Toast.makeText(this,"备份已保存",Toast.LENGTH_SHORT).show();}
                catch(IOException e){Toast.makeText(this,"备份保存失败",Toast.LENGTH_LONG).show();}
            }pendingExport=null;
        }
    }
    @Override protected void onSaveInstanceState(Bundle s){super.onSaveInstanceState(s);s.putString("pendingSchool",pendingSchool);s.putString("pendingExport",pendingExport);}
    @Override public void onBackPressed(){if(web.canGoBack())web.goBack();else super.onBackPressed();}
    @Override protected void onDestroy(){if(fileCallback!=null)fileCallback.onReceiveValue(null);web.destroy();super.onDestroy();}
    private static WebResourceResponse response(int code,String reason,String mime,byte[] b){return new WebResourceResponse(mime,"UTF-8",code,reason,java.util.Collections.emptyMap(),new ByteArrayInputStream(b));}
    private static String mime(String p){String s=p.toLowerCase(Locale.ROOT);if(s.endsWith(".js"))return "text/javascript";if(s.endsWith(".css"))return "text/css";if(s.endsWith(".png"))return "image/png";if(s.endsWith(".svg"))return "image/svg+xml";if(s.endsWith(".webmanifest"))return "application/manifest+json";return "text/html";}
}
