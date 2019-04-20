package com.example.move;

import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class LeaderBoardActivity extends AppCompatActivity {

    TextView leaderboard;
    Button goback;
    DatabaseHelper myDb;
    DailyDistanceDB userDb;
    String formattedDate;
    Main2Activity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leader_board);
        leaderboard = findViewById(R.id.board);
        goback = findViewById(R.id.button3);
        getStatics();
        setGoback();
        Intent intent = getIntent();
        String username = intent.getStringExtra("SENDTOTHIRDACTIVITY");
        Intent back = new Intent();
        back.putExtra("SENDBACKTOSECONDACTIVITY",username);
        setResult(RESULT_OK,back);
    }

    public void setGoback(){
        goback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    public void getStatics(){
        myDb = MainActivity.myDb;
        instance = Main2Activity.instance;
        formattedDate = Main2Activity.formattedDate;
        Cursor myDbCursor = myDb.getAllData();
        pairLinkedList allUser = new pairLinkedList();
        while(myDbCursor.moveToNext()){
            String username = myDbCursor.getString(0);
            userDb = new DailyDistanceDB(instance,username);
            double distance = userDb.getDistance(formattedDate);
            userDistancePair pair = new userDistancePair(username, distance);
            allUser.insert(pair);
        }
        userDistancePair tmp = allUser.head;
        while(tmp!=null){
            leaderboard.append(tmp.username+":  "+tmp.distance+" feet\n");
            tmp = tmp.next;
        }
    }

    public class userDistancePair{
        private String username;
        private double distance;
        private userDistancePair prev;
        private userDistancePair next;
        userDistancePair(String u, double dis){
            username = u;
            distance = dis;
            next = null;
            prev = null;
        }
    }

    public class pairLinkedList{
        userDistancePair head;
        pairLinkedList(){
            head = null;
        }
        public void insert(userDistancePair p){
            if(head == null){
                head = p;
            }else{
                userDistancePair tmp = head;
                while((tmp.distance>p.distance)&&(tmp.next!=null)){
                    tmp = tmp.next;
                }
                while((tmp.distance==p.distance)&&tmp.username.compareTo(p.username)<0&&(tmp.next!=null)){
                    tmp = tmp.next;
                }
                if(tmp.distance>p.distance){
                    tmp.next = p;
                    p.prev = tmp;
                }else if((tmp.distance==p.distance)&&tmp.username.compareTo(p.username)<0){
                    tmp.next = p;
                    p.prev = tmp;
                }else{
                    if(tmp.prev!=null){
                        tmp.prev.next = p;
                        p.prev = tmp.prev;
                        p.next = tmp;
                        tmp.prev = p;
                    }else{
                        p.next = tmp;
                        tmp.prev = p;
                        head = p;
                    }
                }
            }
        }
    }
}
