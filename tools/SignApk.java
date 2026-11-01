import com.android.apksig.ApkSigner;
import com.android.apksig.ApkVerifier;
import java.io.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;

public class SignApk {
    public static void main(String[] args)throws Exception {
        KeyStore store=KeyStore.getInstance("PKCS12");
        try(InputStream in=new FileInputStream(args[0])){store.load(in,"android".toCharArray());}
        PrivateKey key=(PrivateKey)store.getKey("qingke-debug","android".toCharArray());
        X509Certificate cert=(X509Certificate)store.getCertificate("qingke-debug");
        ApkSigner.SignerConfig config=new ApkSigner.SignerConfig.Builder("qingke-debug",key,Collections.singletonList(cert)).build();
        new ApkSigner.Builder(Collections.singletonList(config)).setInputApk(new File(args[1])).setOutputApk(new File(args[2])).setMinSdkVersion(26).setV1SigningEnabled(true).setV2SigningEnabled(true).setV3SigningEnabled(true).build().sign();
        ApkVerifier.Result result=new ApkVerifier.Builder(new File(args[2])).setMinCheckedPlatformVersion(26).build().verify();
        if(!result.isVerified())throw new Exception("APK signature verification failed: "+result.getErrors());
        System.out.println("APK signatures verified (v2="+result.isVerifiedUsingV2Scheme()+", v3="+result.isVerifiedUsingV3Scheme()+")");
    }
}
