package com.example.move;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DailyDistanceDB extends SQLiteOpenHelper {
    public static final String TABLE_NAME = "distance_table";
    public static final String COL_1 = "Date";
    public static final String COL_2 = "Distance";

    public DailyDistanceDB(Context context, String username) {
        super(context, username+".db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table "+ TABLE_NAME + " (Date TEXT PRIMARY KEY, Distance INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean insertDistance(String date, Float distance){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_1,date);
        cv.put(COL_2,distance);
        long result = db.insert(TABLE_NAME, null, cv);
        if(result==-1){
            return false;
        }
        return true;
    }

    public boolean checkDateExist(String date){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor check = db.rawQuery("select * from "+TABLE_NAME+" where Date = ?", new String[]{date});
        if(check.getCount()==0){
            return false;
        }
        return  true;
    }

    public float getDistance(String date){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor dateDistance = db.rawQuery("select * from "+TABLE_NAME+" where Date = ?", new String[]{date});
        dateDistance.moveToFirst();
        float distance = dateDistance.getFloat(1);
        return distance;
    }

    public boolean updateDistance(String date, Float distance){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_1,date);
        cv.put(COL_2,distance);
        long affected = db.update(TABLE_NAME, cv, "Date = ?", new String[]{date});
        if(affected==0){
            return false;
        }
        return true;
    }

    public Cursor getAllData(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from "+TABLE_NAME,null);
        return res;
    }
}
