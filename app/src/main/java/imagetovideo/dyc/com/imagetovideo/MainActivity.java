package imagetovideo.dyc.com.imagetovideo;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},123);
        if(PermissionChecker.checkSelfPermission(getApplicationContext(),Manifest.permission.WRITE_EXTERNAL_STORAGE)==PermissionChecker.PERMISSION_GRANTED){

//                  ImageToVideoUtil imageToVideoUtil = new ImageToVideoUtil();
//                  imageToVideoUtil.init(320,480);
//                  imageToVideoUtil.encode(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.bg_title_red_bg),320,480,false),3);
//                  imageToVideoUtil.finish();
                    final ImageToVideoUtil2 imageToVideoUtil2 = new ImageToVideoUtil2();
                    imageToVideoUtil2.init(320,480);
                    for(int i=0;i<16*3;i++) {
                        imageToVideoUtil2.drawframe(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.bg_title_red_bg), 320, 480, false), i);
                    }
            imageToVideoUtil2.drainEnd();
                    Toast.makeText(getApplicationContext(),"编码完成",Toast.LENGTH_LONG).show();
        }
    }
}
