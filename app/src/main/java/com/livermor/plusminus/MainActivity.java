package com.livermor.plusminus;

import android.graphics.Typeface;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity
        implements Game.ResultsCallback, MyButton.MyOnClickListener {

    private static final int MATRIX_SIZE = 5;// можете ставить от 2 до 20))

    //ui
    private TextView mUpText, mLowText;
    GridLayout mGridLayout;
    private MyButton[][] mButtons;

    private Game game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGridLayout = (GridLayout) findViewById(R.id.my_grid);
        mGridLayout.setColumnCount(MATRIX_SIZE);
        mGridLayout.setRowCount(MATRIX_SIZE);
        mButtons = new MyButton[MATRIX_SIZE][MATRIX_SIZE];//5 строк и 5 рядов

        //создаем кнопки для цифр
        for (int yPos = 0; yPos < MATRIX_SIZE; yPos++) {
            for (int xPos = 0; xPos < MATRIX_SIZE; xPos++) {
                MyButton mBut = new MyButton(this, xPos, yPos);

                mBut.setTextSize(30-MATRIX_SIZE);
                Typeface boldTypeface = Typeface.defaultFromStyle(Typeface.BOLD);
                mBut.setTypeface(boldTypeface);
                mBut.setTextColor(ContextCompat.getColor(this, R.color.white));
                mBut.setOnClickListener(this);
                mBut.setPadding(1, 1, 1, 1); //так цифры будут адаптироваться под размер

                mBut.setAlpha(1);
                mBut.setClickable(false);

                mBut.setBackgroundResource(R.drawable.bg_grey);

                mButtons[yPos][xPos] = mBut;
                mGridLayout.addView(mBut);
            }
        }

        mUpText = (TextView) findViewById(R.id.upper_scoreboard);
        mLowText = (TextView) findViewById(R.id.lower_scoreboard);

        //расположим кнопки с цифрами равномерно внутри mGridLayout
        mGridLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        setButtonsSize();
                        //нам больше не понадобится OnGlobalLayoutListener
                        mGridLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });

        game = new Game(this, MATRIX_SIZE); //создаем класс игры
        game.startGame(); //и запускаем ее

    }//onCreate

    private void setButtonsSize() {
        int pLength;
        final int MARGIN = 6;

        int pWidth = mGridLayout.getWidth();
        int pHeight = mGridLayout.getHeight();
        int numOfCol = MATRIX_SIZE;
        int numOfRow = MATRIX_SIZE;

        //сделаем mGridLayout квадратом
        if (pWidth >= pHeight) pLength = pHeight;
        else pLength = pWidth;
        ViewGroup.LayoutParams pParams = mGridLayout.getLayoutParams();
        pParams.width = pLength;
        pParams.height = pLength;
        mGridLayout.setLayoutParams(pParams);

        int w = pLength / numOfCol;
        int h = pLength / numOfRow;

        for (int yPos = 0; yPos < MATRIX_SIZE; yPos++) {
            for (int xPos = 0; xPos < MATRIX_SIZE; xPos++) {
                GridLayout.LayoutParams params = (GridLayout.LayoutParams)
                        mButtons[yPos][xPos].getLayoutParams();
                params.width = w - 2 * MARGIN;
                params.height = h - 2 * MARGIN;
                params.setMargins(MARGIN, MARGIN, MARGIN, MARGIN);
                mButtons[yPos][xPos].setLayoutParams(params);
                //Log.w(TAG, "process goes in customizeMatrixSize");
            }
        }
    }

    //MyButton.MyOnClickListener интерфейс
    //*************************************************************************
    @Override
    public void OnTouchDigit(MyButton v) {
        game.OnUserTouchDigit(v.getIdY(), v.getIdX());
    }

    //Game.ResultsCallback интерфейс
    //*************************************************************************
    @Override
    public void changeLabel(boolean upLabel, int points) {
        if (upLabel) mUpText.setText(String.format("Бот: %d", points));
        else mLowText.setText(String.valueOf(String.format("Вы: %d", points)));
    }

    @Override
    public void changeButtonBg(int y, int x, boolean row, boolean active) {

        if (active) {
            if (row) mButtons[y][x].setBackgroundResource(R.drawable.bg_blue);
            else mButtons[y][x].setBackgroundResource(R.drawable.bg_red);

        } else {
            mButtons[y][x].setBackgroundResource(R.drawable.bg_grey);
        }
    }

    @Override
    public void setButtonText(int y, int x, int text) {
        mButtons[y][x].setText(String.valueOf(text));
    }

    @Override
    public void changeButtonClickable(int y, int x, boolean clickable) {
        mButtons[y][x].setClickable(clickable);
    }

    @Override
    public void onResult(int playerOnePoints, int playerTwoPoints) {

        String text;
        if (playerOnePoints > playerTwoPoints) text = "вы победили";
        else if (playerOnePoints < playerTwoPoints) text = "бот победил";
        else text = "ничья";

        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();

        //через 1500 миллисекунд выполним метод run
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                recreate(); //начать новую игру — пересоздать класс MainActivity
            }
        }, 1500);
    }

    @Override
    public void onClick(final int y, final int x, final boolean flyDown) {

        final Button currentBut = mButtons[y][x];

        currentBut.setAlpha(0.7f);
        currentBut.setClickable(false);

        AnimationSet sets = new AnimationSet(false);
        int direction = flyDown ? 400 : -400;
        TranslateAnimation animTr = new TranslateAnimation(0, 0, 0, direction);
        animTr.setDuration(810);
        AlphaAnimation animAl = new AlphaAnimation(0.4f, 0f);
        animAl.setDuration(810);
        sets.addAnimation(animTr);
        sets.addAnimation(animAl);
        currentBut.startAnimation(sets);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                currentBut.clearAnimation();
                currentBut.setAlpha(0);
            }
        }, 800);
    }
}
