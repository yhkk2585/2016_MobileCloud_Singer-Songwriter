package com.songDatabase;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import com.RecyclerUtil.SongRecyclerAdapter;
import com.RecyclerUtil.RecyclerItemClickListener;
import com.mysampleapp.MainActivity;
import com.mysampleapp.R;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class SongListViewActivity extends Activity  {

    static Context mycontect;
    public static Context getContext()
    {
        return mycontect;
    }
    String selectFilepath[];
    String contents[];
    String listUsername[];
    String listSongname[];
    String listUserID[];
    int commentCount[];
    int likeCount[];
    boolean rankingFunction=false;
    songDB songDBs[];

    List<songData> rowItems;

    SongRecyclerAdapter adapter;
    RecyclerView recyclerView;

    MediaPlayer mp;
    SeekBar seekBar;
    TextView text;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_songlistview);
        mycontect=getApplication();

        Intent intent = getIntent();
        String ranking = intent.getStringExtra("ranking");
        if(ranking.equals("TRUE")) rankingFunction=true;
        else rankingFunction=false;

        recyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        setList();

        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);

        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(getApplicationContext(), recyclerView, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                alertShow(position);
            }

            @Override
            public void onItemLongClick(View view, int position) {
                // ...
            }
        }));
        // 아이템을 [클릭]시의 이벤트 리스너를 등록

        mp = new MediaPlayer();

        seekBar = (SeekBar)findViewById(R.id.playbar_song);
        text=(TextView)findViewById(R.id.text1_song);
        seekBar.setVisibility(ProgressBar.VISIBLE);


    }


    @Override
    public void onBackPressed()
    {
        Intent backtoComment = new Intent();
        setResult(RESULT_OK, backtoComment);
        mp.stop();
        mp.seekTo(0);
        finish();
    }


    public void setListView(String songList) {

        rowItems = new ArrayList<songData>();
        String[] songListArray = songList.split("--:--");             // 전체 테이블 받아옴.

        listSongname = new String[songListArray.length];
        listUsername = new String[songListArray.length];
        listUserID = new String[songListArray.length];
        selectFilepath = new String[songListArray.length];
        contents = new String[songListArray.length];
        commentCount = new int[songListArray.length];
        likeCount = new int[songListArray.length];
        songDBs = new songDB[songListArray.length-1]; // bound 조절.


        for (int k = 0; k < songListArray.length - 1; k++) {
            String listArray[];
            listArray = songListArray[k].split(":::");

            listUsername[k] = listArray[1];
            listUserID[k] = listArray[2];
            listSongname[k] = listArray[3];
            contents[k] = listArray[4];
            selectFilepath[k] = listArray[5];
            commentCount[k] = Integer.parseInt(listArray[6]);
            likeCount[k] = Integer.parseInt(listArray[7]);
            songData item = new songData(listSongname[k], contents[k], listUsername[k],
                    "" + likeCount[k],listUserID[k]);
            if(!rankingFunction) rowItems.add(item);
            else
            {
                songDBs[k]=new songDB( listUsername[k],listUserID[k],listSongname[k],
                        contents[k], commentCount[k], likeCount[k]);
            }
        }

        if(rankingFunction)    // 좋아요 카운트를 소팅하면서 나머지 다른 것들도같이 소팅함  모든걸 다 비교하려면 시간이 오래걸리는데?
        {
            Arrays.sort(songDBs);
            int kCount;
            if(songListArray.length-1<7) kCount=songListArray.length-1;
            else kCount=7;
            for (int k = 0; k <kCount; k++) {
                listUsername[k]=songDBs[k].username;
                listUserID[k]=songDBs[k].userid;
                listSongname[k]=songDBs[k].songname;
                contents[k]=songDBs[k].comment;
                commentCount[k]=songDBs[k].commentcount;
                likeCount[k]=songDBs[k].likecount;
                songData item = new songData(listSongname[k], contents[k], listUsername[k],
                        "" + likeCount[k],listUserID[k]);
                rowItems.add(item);
            }
        }

        MainActivity.UserIDClass.setRecyclerAdapterType("SongList");
        adapter = new SongRecyclerAdapter(this, rowItems);
        recyclerView.setAdapter(adapter);

    }

    public void alertShow(int position){

        AlertDialog.Builder alert = new AlertDialog.Builder(SongListViewActivity.this);

        final String UserName = listUsername[position];
        final String SongName = listSongname[position];
        final String UserID = listUserID[position];
        final int commentCounts = commentCount[position];
        final int likeCounts = likeCount[position];
        final CharSequence[] items = {"바로듣기", "다운받기", "좋아요" + " (" + likeCounts + ")", "댓글보기" + " (" + commentCounts + ")", "취소"};

        alert.setTitle(SongName + " : " + contents[position])
                .setItems(items, new DialogInterface.OnClickListener() {                               ///메뉴선택별 기능 추가
                    public void onClick(DialogInterface dialog, int index) {
                        if (index==0)
                        {
                            mp.stop();
                            mp.seekTo(0);
                            try {
                                startSongStream(SongName, UserID, UserName);
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }

                        }
                        else if (index == 1) {
                            Intent intent = new Intent(SongListViewActivity.this, onlyDownloadActivity.class);
                            intent.putExtra("key", UserName + "_" + SongName + ".mp4");
                            startActivity(intent);
                        } else if (index == 2) {                                                              //좋아요
                            UploadDatabaseManager myLikeDB = new UploadDatabaseManager();
                            myLikeDB.upLikeCount(getApplicationContext(), SongName, UserID);
//                                    if(MainActivity.UserIDClass.getLikeTrue()) setList(); // 좋아요 성공하면 최신화
                            setList();

                        } else if (index == 3) {                                                           //댓글보기
                            // dbContents intent 해서 보여주기. 여기서는 userName,userID, songName 일치하는 댓글 보여주기 ........... 만약 곡 이름이 같다면 업로드 못하게 설정.
                                    showComment(UserID, SongName);
                                }
                        else if (index == 4) {                                                            //취소
                                    // 아무것도 하지 않음
                        }
                    }
                });
        alert.show();

    }
    private void startSongStream(String SongName, String UserID, String UserName) throws UnsupportedEncodingException {

        try {
            SongName=URLEncoder.encode(SongName,"UTF-8");
            UserName=URLEncoder.encode(UserName,"UTF-8");
            Log.v("AUDIOHTTPPLAYER", "https://s3.amazonaws.com/mysongs3/"+UserName+"_"+SongName+".mp4");
            mp.setDataSource("https://s3.amazonaws.com/mysongs3/"+UserName+"_"+SongName+".mp4");
            mp.prepare();

            seekBar.setMax(mp.getDuration());

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        mp.seekTo(progress);
                    }
                    int m = progress / 60000;
                    int s = (progress % 60000) / 1000;
                    String strTime = String.format("%02d:%02d", m, s);
                    text.setText(strTime);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
            mp.start();
            Thread();

        } catch (IOException e) {
            Log.v("AUDIOHTTPPLAYER", e.getMessage());
        }
    }


    public void Thread(){
        Runnable task = new Runnable(){


            public void run(){
                // 음악이 재생중일때
                while(mp.isPlaying()){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    seekBar.setProgress(mp.getCurrentPosition());
                }
            }
        };
        Thread thread = new Thread(task);
        thread.start();
    }



    private void setList() {

        class LoginAsync extends AsyncTask<String, Void, String>{

            private Dialog loadingDialog;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loadingDialog = ProgressDialog.show(SongListViewActivity.this, "Please wait", "Loading...");
            }

            @Override
            protected String doInBackground(String... params) {
                InputStream is = null;
                String result = null;

                try{
                    HttpClient httpClient = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost(
                            "http://52.207.214.66/singersong/songListView.php");
                    HttpResponse response = httpClient.execute(httpPost);

                    HttpEntity entity = response.getEntity();

                    is = entity.getContent();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8);
                    StringBuilder sb = new StringBuilder();

                    String line = null;
                    while ((line = reader.readLine()) != null)
                    {
                        sb.append(line + "\n");
                    }
                    result = sb.toString();
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return result;
            }

            @Override
            protected void onPostExecute(String result){
               String s = result.trim();
                loadingDialog.dismiss();
                if(s.charAt(0)==':'){

                    // add set list view
                    setListView(result);

                }else {
                    Toast.makeText(getApplicationContext(), "Invalid User Name or Password", Toast.LENGTH_LONG).show();
                }
            }
        }

        LoginAsync la = new LoginAsync();
        la.execute();

    }


    private void showComment(String UserID, String SongName)
    {
        Intent intent = new Intent(SongListViewActivity.this,ShowCommentActivity.class);
        intent.putExtra("SongName", SongName);
        intent.putExtra("UserID", UserID);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {

            setList();
        }
    }

}

class songDB implements Comparable{
    public String username;
    public String songname;
    public String userid;
    public String comment;
    public int commentcount;
    public int likecount;

    public songDB(String username,String userid, String songname,String comment,int commentcount,int likecount)
    {
        this.username=username;
        this.userid=userid;
        this.songname=songname;
        this.comment=comment;
        this.commentcount=commentcount;
        this.likecount=likecount;
    }
    public int compareTo(Object obj){
        songDB other = (songDB) obj;
        if(likecount <other.likecount) return 1;             // 내림차순정리
        else if(likecount > other.likecount) return -1;
        else return 0;
    }
}