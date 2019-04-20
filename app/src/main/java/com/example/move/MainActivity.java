package com.example.move;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
/*
*
*
*
*
* */
public class MainActivity extends AppCompatActivity {

    private EditText username;
    private EditText password;
    private Button create;
    private Button login;

    static DatabaseHelper myDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myDb = new DatabaseHelper(this);

        username = (EditText)findViewById(R.id.editText);
        password = (EditText)findViewById(R.id.editText2);
        create = (Button)findViewById(R.id.button);
        login = (Button)findViewById(R.id.button2);

        createAccount();
        Login();
    }

    public void createAccount(){
        create.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                String usrn = username.getText().toString();
                boolean accountExists = myDb.checkAccountExist(usrn);
                if(accountExists){
                    Toast.makeText(MainActivity.this, "Account exists", Toast.LENGTH_LONG).show();
                }else{
                    boolean isCreated = myDb.createAccount(usrn,
                            password.getText().toString());
                    if(isCreated){
                        Toast.makeText(MainActivity.this, "Account Created", Toast.LENGTH_LONG).show();

                    }else{
                        Toast.makeText(MainActivity.this, "Create Account failed", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    public void Login(){
        login.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                String usrn = username.getText().toString();
                boolean accountExists = myDb.checkAccountExist(usrn);
                boolean passwordMatch = myDb.checkAccount(usrn,
                        password.getText().toString());
                if(accountExists){
                    if(passwordMatch){
                        Intent intent = new Intent(MainActivity.this, Main2Activity.class);
                        intent.putExtra("SENDTOSECONDACTIVITY",usrn);
                        startActivity(intent);
                    }else{
                        Toast.makeText(MainActivity.this, "Password incorrect!", Toast.LENGTH_LONG).show();
                    }

                }else{
                    Toast.makeText(MainActivity.this, "Account not exists", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
