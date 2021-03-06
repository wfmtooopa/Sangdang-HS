package com.dongyun.sangdang.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.dongyun.sangdang.R;
import com.dongyun.sangdang.util.DevLog;
import com.dongyun.sangdang.util.ImageDownloader;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class EventsContents extends ActionBarActivity {
    TextView tvTitle, tvDate, tvAuthor, tvContents, tvFile;
    CardView cvFile;

    String cons = "", filename = "";
    private NoticeOpenTask noticeTask;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contents);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (!isNetworkConnected(this)) {
            new AlertDialog.Builder(this)
                    .setIcon(R.drawable.ic_error)
                    .setTitle("네트워크 연결")
                    .setMessage("\n네트워크 연결 상태 확인 후 다시 시도해 주십시요\n")
                    .setPositiveButton("확인",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    finish();
                                }
                            })
                    .show();
        } else {
            tvTitle = (TextView) findViewById(R.id.tv_title);
            tvDate = (TextView) findViewById(R.id.tv_date);
            tvAuthor = (TextView) findViewById(R.id.tv_author);
            tvContents = (TextView) findViewById(R.id.tv_contents);
            tvFile = (TextView) findViewById(R.id.tv_file);
            cvFile = (CardView) findViewById(R.id.card_file);
            Intent in = getIntent();
            tvTitle.setText(in.getStringExtra("title"));
            tvDate.setText("등록일 : " + in.getStringExtra("date"));
            tvAuthor.setText("작성자 : " + in.getStringExtra("author"));
            noticeTask = new NoticeOpenTask();
            noticeTask.execute(in.getStringExtra("URL"), "", "");
        }
    }

    // 인터넷 연결 상태 체크
    public boolean isNetworkConnected(Context context) {
        boolean isConnected = false;

        ConnectivityManager manager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mobile = manager
                .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo wifi = manager
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (mobile.isConnected() || wifi.isConnected()) {
            isConnected = true;
        } else {
            isConnected = false;
        }
        return isConnected;
    }


    // AsyncTask<Params,Progress,Result>
    private class NoticeOpenTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... urls) {

            if (!urls[0].equals("")) {

                // Notices URL
                try {
                    // 파싱할 페이지 URL

                    Document doc = Jsoup.connect(urls[0]).get();
                    Elements rawcontents = doc.select("#m_mainView tbody tr#m_content td");
                    DevLog.i("RAWCON", rawcontents.toString());

                    Elements rawimage = doc.select("#m_mainView tbody tr#m_content td p img");

                    String imagesrc = "";
                    for (Element str : rawimage) {
                        imagesrc = imagesrc + str.attr("src") + "\n";
                    }

                    Elements rawfile = doc.select("#m_mainView tbody tr:eq(3) td p a");
                    for (Element el : rawfile) {
                        String filedata = "http://sangdang.hs.kr" + el.attr("href");
                        String filetitle = el.text();
                        filename = filename + "<a href=\""
                                + filedata.replace("/common/", "/m/") //모바일 다운로드시 인코딩문제 해결
                                + "\">" + filetitle + "</a>"  +"<br>";
                    }
                    String[] imgs = imagesrc.split("\n");
                    int[] a = {R.id.tvimg1, R.id.tvimg2, R.id.tvimg3, R.id.tvimg4, R.id.tvimg5};
                    for (int i = 0; i < imgs.length; i++) {
                        if (i == 5) break; //최대 5개
                        imgs[i] = "http://sangdang.hs.kr" + imgs[i]; //img src 앞에 학교주소 붙임
                        final ImageView imgView = (ImageView) findViewById(a[i]);
                        new ImageDownloader(imgView).execute(imgs[i]);
                        imgView.post(new Runnable() { //Wrong Thread Exception 예외 처리
                            public void run() {
                                imgView.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                    return rawcontents.toString().replace("<img", "<p"); //img 부분 삭제
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return cons;
        }

        @Override
        protected void onPostExecute(final String cons) {
            // TODO Auto-generated method stub
            super.onPostExecute(cons);
            tvContents.post(new Runnable() { //Wrong Thread Exception 예외 처리
                public void run() {
                    tvContents.setText(Html.fromHtml(cons));
                    tvContents.setMovementMethod(LinkMovementMethod.getInstance()); //href Clickable
                    if(!filename.equals("")) {
                        cvFile.setVisibility(View.VISIBLE);
                        tvFile.setText(Html.fromHtml(filename));
                        tvFile.setMovementMethod(LinkMovementMethod.getInstance());
                    }
                }
            });
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_webview, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.webview_menu:
                Intent in = getIntent();
                Intent intent = new Intent(EventsContents.this,
                        WebViewActivity.class);
                intent.putExtra("URL", in.getStringExtra("URL"));
                startActivity(intent);
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
