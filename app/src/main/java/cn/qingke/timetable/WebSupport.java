package cn.qingke.timetable;

import android.app.Activity;
import android.app.AlertDialog;
import android.webkit.*;
import android.widget.EditText;

final class WebSupport {
    static void configure(WebView web) {
        WebSettings s=web.getSettings();
        s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false); s.setAllowContentAccess(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setMediaPlaybackRequiresUserGesture(true);
        s.setSafeBrowsingEnabled(true);
        // No addJavascriptInterface: school pages never receive a native object.
    }
    static WebChromeClient dialogs(Activity activity) {
        return new WebChromeClient() {
            @Override public boolean onJsAlert(WebView v,String url,String message,JsResult result) {
                new AlertDialog.Builder(activity).setMessage(message).setPositiveButton("确定",(d,w)->result.confirm()).setOnCancelListener(d->result.cancel()).show();return true;
            }
            @Override public boolean onJsConfirm(WebView v,String url,String message,JsResult result) {
                new AlertDialog.Builder(activity).setMessage(message).setPositiveButton("确定",(d,w)->result.confirm()).setNegativeButton("取消",(d,w)->result.cancel()).setOnCancelListener(d->result.cancel()).show();return true;
            }
            @Override public boolean onJsPrompt(WebView v,String url,String message,String value,JsPromptResult result) {
                EditText input=new EditText(activity); input.setText(value);
                new AlertDialog.Builder(activity).setMessage(message).setView(input).setPositiveButton("确定",(d,w)->result.confirm(input.getText().toString())).setNegativeButton("取消",(d,w)->result.cancel()).setOnCancelListener(d->result.cancel()).show();return true;
            }
        };
    }
}
