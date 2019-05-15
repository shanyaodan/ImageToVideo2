package imagetovideo.dyc.com.imagetovideo;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import java.io.IOException;

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
//            new Thread(){
//                @Override
//                public void run() {
//                    super.run();
//                    final ImageToVideoUtil2 imageToVideoUtil2 = new ImageToVideoUtil2();
//                    imageToVideoUtil2.init(320,480);
//                   Bitmap bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.bg_title_red_bg), 320, 480, false);
//                    for(int i=0;i<16*3000/1000;i++) {
//                        imageToVideoUtil2.drawframe(bitmap, i);
//                    }
//                    imageToVideoUtil2.drainEnd();
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Toast.makeText(getApplicationContext(),"编码完成",Toast.LENGTH_LONG).show();
//                        }
//                    });
//                }
//            }.start();

clipVideo(0);
        }
    }


    private void clipVideo(final int num){

        new Thread(){
            @Override
            public void run() {
                super.run();

                VideoClipper videoClipper = new VideoClipper();
                videoClipper.setInputVideoPath(Environment.getExternalStorageDirectory().getAbsolutePath()+"/bbbb.mp4");
                videoClipper.setOutputVideoPath(Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+(num==1?"o":"")+"outbbbb.mp4");
                videoClipper.setOnVideoCutFinishListener(new VideoClipper.OnVideoCutFinishListener() {
                    @Override
                    public void onFinish() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(),"裁剪完成",Toast.LENGTH_LONG).show();
                            }
                        });
                        if(num==0){
                            clipVideo(1);
                        }
                    }

                    @Override
                    public void onError() {

                    }
                });
                try {
                    videoClipper.clipVideo(0,2000000);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }
}
