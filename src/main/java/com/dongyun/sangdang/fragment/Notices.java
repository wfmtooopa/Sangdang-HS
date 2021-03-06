package com.dongyun.sangdang.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.dongyun.sangdang.util.DevLog;
import com.dongyun.sangdang.activity.NoticesContents;
import com.dongyun.sangdang.util.PostListAdapter;
import com.dongyun.sangdang.R;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;


public class Notices extends Fragment {

    private ArrayList<String> titlearray;
    private ArrayList<String> titleherfarray;
    private ArrayList<String> authorarray;
    private ArrayList<String> datearray;
    private PostListAdapter adapter;
    private SwipeRefreshLayout SRL;
    ListView listview;

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        View view = inflater.inflate(R.layout.activity_notices, null);
        if (!isNetworkConnected(getActivity())) {
            new AlertDialog.Builder(getActivity())
                    .setIcon(R.drawable.ic_error)
                    .setTitle("네트워크 연결")
                    .setMessage("\n네트워크 연결 상태 확인 후 다시 시도해 주십시요\n")
                    .setPositiveButton("확인",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    getActivity().finish();
                                }
                            }).show();
        } else {
            listview = (ListView) view.findViewById(R.id.listView);
            SRL = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
            SRL.setColorSchemeColors(Color.rgb(231, 76, 60),
                    Color.rgb(46, 204, 113), Color.rgb(41, 128, 185),
                    Color.rgb(241, 196, 15));
            SRL.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    networkTask();
                }
            });
            networkTask();
        }
        return view;
    }

    private AdapterView.OnItemClickListener GoToWebPage = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> adapterView, View clickedView,
                                int pos, long id) {
            try {
                String herfitem = titleherfarray.get(pos);
                DevLog.d("Herf", herfitem);
                String title = titlearray.get(pos);
                String date = datearray.get(pos);
                String author = authorarray.get(pos);

                Intent intent = new Intent(getActivity(),
                        NoticesContents.class);
                intent.putExtra("URL", herfitem);
                intent.putExtra("title", title);
                intent.putExtra("date", date);
                intent.putExtra("author", author);
                startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    };

    // Method for get list of notices
    private void networkTask() {
        final Handler mHandler = new Handler();
        new Thread() {

            public void run() {

                mHandler.post(new Runnable() {

                    public void run() {
                        SRL.setRefreshing(true);
                    }
                });

                // Task

                // Notices URL
                try {
                    titlearray = new ArrayList<String>();
                    titleherfarray = new ArrayList<String>();
                    authorarray = new ArrayList<String>();
                    datearray = new ArrayList<String>();
                    // 파싱할 페이지 URL
                    Document doc = Jsoup
                            .connect(
                                    "http://sangdang.hs.kr/index.jsp?SCODE=S0000000206&mnu=M001011011")
                            .get();
                    Elements rawmaindata = doc.select("#m_mainList tbody tr td div.m_ltitle a");
                    Elements rawauthordata = doc.select("td:eq(2)");
                    Elements rawdatedata = doc.select("td:eq(3)");
                    String titlestring = rawmaindata.toString();
                    DevLog.i("Notices", "Parsed Strings" + titlestring);

                    // 파싱할 데이터로 배열 생성
                    for (Element el : rawmaindata) {
                        String titlherfedata = el.attr("href");
                        String titledata = el.attr("title");
                        //titlherfedata = ("http://sangdang.hs.kr"
                        //        + titlherfedata).replace("index.jsp", "m/mainView.jsp") + "&cmd=view";
                        //titleherfarray.add(titlherfedata);
                        titleherfarray.add("http://www.sangdang.hs.kr"
                                + titlherfedata); // add value to ArrayList
                        titlearray.add(titledata); // add value to ArrayList
                    }
                    DevLog.i("Notices", "Parsed Link Array Strings"
                            + titleherfarray);
                    DevLog.i("Notices", "Parsed Array Strings" + titlearray);

                    for (Element el : rawauthordata) {
                        String authordata = el.text();
                        DevLog.d("Author", el.text());
                        authorarray.add(authordata);
                    }
                    for (Element el : rawdatedata) {
                        String datedata = el.text();
                        DevLog.d("Date", el.text());
                        datearray.add(datedata);
                    }


                } catch (IOException e) {
                    e.printStackTrace();

                }

                mHandler.post(new Runnable() {
                    public void run() {
                        // UI Task
                        // 배열로 어뎁터 설정
                        adapter = new PostListAdapter(getActivity(), titlearray,
                                datearray, authorarray);
                        listview.setAdapter(adapter);
                        listview.setOnItemClickListener(GoToWebPage);
                        handler.sendEmptyMessage(0);
                        SRL.setRefreshing(false);
                    }
                });

            }
        }.start();
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

}
