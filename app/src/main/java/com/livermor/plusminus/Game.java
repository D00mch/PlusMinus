package com.livermor.plusminus;

import android.os.AsyncTask;
import android.os.Handler;

import java.util.Random;

public class Game {

    //время задержки перед обновлениями очков, смены анимации
    public static final int mTimeToWait = 800;
    protected MyAnimation mAnimation; //класс AsyncTask для анимации

    //матрица цифр и матрица допустимых ходов
    protected int[][] mMatrix; //digits for buttons
    protected volatile boolean[][] mAllowedMoves;
    protected int mSize; //размер матрицы

    protected int playerOnePoints = 0, playerTwoPoints = 0;//очки игроков

    protected volatile boolean isRow = true; //мы играем за строку или за ряд
    protected volatile int currentActiveNumb; //нужно для определения последнего хода
    protected ResultsCallback mResults;//интерфейс, который будет реализовывать MainActivity

    protected volatile Bot bot;//написанный нами бот
    Random rnd; // для заполнения матрицы цифрами и определения первой активной строки

    public Game(ResultsCallback results, int size) {
        mResults = results; //передаем сущность интерфейса
        mSize = size;

        rnd = new Random();
        generateMatrix(); //заполняем матрицу случайнами цифрами

        //условный ход, нужен для определения активной строки
        currentActiveNumb = rnd.nextInt(mSize);

        isRow = true; //в нашей версии мы всегда будем играть за строку (просто для упрощения)

        for (int yPos = 0; yPos < mSize; yPos++) {
            for (int xPos = 0; xPos < mSize; xPos++) {

                //записываем сгенерированные цифры на кнопки с помощью нашего интерфейса
                mResults.setButtonText(yPos, xPos, mMatrix[yPos][xPos]);

                if (yPos == currentActiveNumb) // закрашиваем активную строку
                    mResults.changeButtonBg(yPos, xPos, isRow, true);
            }
        }

        bot = new Bot(mMatrix, true);
    }

    public void startGame() {
        activateRawOrColumn(true);
    }

    protected void generateMatrix() {

        mMatrix = new int[mSize][mSize];
        mAllowedMoves = new boolean[mSize][mSize];

        for (int i = 0; i < mSize; i++) {
            for (int j = 0; j < mSize; j++) {

                mMatrix[i][j] = rnd.nextInt(19) - 9; //от -9 до 9
                mAllowedMoves[i][j] = true; // сперва все ходы доступны
            }
        }
    }

    //будем вызывать метод из MainActivity, которая будет следить за нажатиями кнопок с цифрами
    public void OnUserTouchDigit(int y, int x) {

        mResults.onClick(y, x, true);
        activateRawOrColumn(false);//после хода нужно заблокирвоать доступные кнопки

        mAllowedMoves[y][x] = false; //два раза в одно место ходить нельзя
        playerOnePoints += mMatrix[y][x]; //берем из матрицы очки

        mResults.changeLabel(false, playerOnePoints);//изменяем свои очки

        mAnimation = new MyAnimation(y, x, true, isRow);//включаем анимацию смены хода
        mAnimation.execute();

        isRow = !isRow; //после хода меняем строку на ряд
        currentActiveNumb = x; //по нашему ходу потом будем определять, куда можно ходить боту
    }

    //по завершению анимации разрешаем совершить ход боту
    protected void onAnimationFinished() {

        if (!isRow) {//в нашей версии бот играет только за ряды (вертикально)

            //используем Handler, потому что предстоит работа с ui, который нельзя обновлять
            //не из главного потока. Handel поставит задачу в очередь главного потока
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    botMove(); //
                }
            }, mTimeToWait / 2);

        } else //если сейчас горизонтальный ход, то активируем строку
            activateRawOrColumn(true);
    }

    private void botMove() {

        //получаем ход бота
        int botMove = bot.move(playerOnePoints,
                playerTwoPoints, mAllowedMoves, currentActiveNumb);

        if (botMove == mSize) {//если ход равен размеру матрицы, значит ходов нет
            onResult(); //дергаем метод завершения игры
            return; //досрочно выходим из метода
        }

        int y = botMove; // по рядам ходит бот
        int x = currentActiveNumb;
        mAllowedMoves[y][x] = false;
        playerTwoPoints += mMatrix[y][x];
        mResults.onClick(y, x, false); //имитируем нажатие на кнопку
        mResults.changeLabel(true, playerTwoPoints); //меняем очки бота

        mAnimation = new MyAnimation(y, x, true, isRow); //анимируем смену хода
        mAnimation.execute();

        isRow = !isRow; //меняем столбцы на строки
        currentActiveNumb = botMove; //по ходу бота определим, где теперь будет строка
    }

    protected void activateRawOrColumn(final boolean active) {

        int countMovesAllowed = 0; // для определения, есть ли допустимые ходы

        int y, x;
        for (int i = 0; i < mMatrix.length; i++) {

            y = isRow ? currentActiveNumb : i;
            x = isRow ? i : currentActiveNumb;

            if (mAllowedMoves[y][x]) { //если ход допустим, то
                mResults.changeButtonClickable(y, x, active); //активируем, либо деактивируем его
                countMovesAllowed++; //если переменная останется нулем, то ходов нет
            }
        }
        if (active && countMovesAllowed == 0) onResult();
    }

    //анимация закрашивания кнопок — одна за другой
    //сперва закрашиваем новые ходы — затем стираем предыдущие
    protected class MyAnimation extends AsyncTask<Void, Integer, Void> {

        int timeToWait = 35; //время задержки в миллисекундах
        int y, x;
        boolean activate;
        boolean row;

        protected MyAnimation(int y, int x, boolean activate, boolean row) {
            this.activate = activate;
            this.row = !row;
            this.y = y;
            this.x = x;
        }

        @Override
        protected Void doInBackground(Void... params) {

            int downInc = row ? x - 1 : y - 1;
            int uppInc = row ? x : y;

            if (activate)
                sleep(Game.mTimeToWait);//наш собственный метод для паузы

            if (activate) { //когда активируем ходы, показываем анимацию от точки нажатия к границам
                while (downInc >= 0 || uppInc < mSize) {
                    //Log.i(TAG, "while in Animation");

                    sleep(timeToWait);
                    if (downInc >= 0)
                        publishProgress(downInc--); //метод AsyncTask для отображения прогресса

                    sleep(timeToWait);
                    if (uppInc < mSize)
                        publishProgress(uppInc++);
                }

            } else {//когда деактивируем ходы, показываем анимацию от границ к точке нажатия

                int downInc2 = 0;
                int uppInc2 = mSize - 1;

                while (downInc2 <= downInc || uppInc2 > uppInc) {

                    sleep(timeToWait);
                    if (downInc2 <= downInc) publishProgress(downInc2++);
                    sleep(timeToWait);
                    if (uppInc2 > uppInc) publishProgress(uppInc2--);
                }
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            int numb = values[0];

            int yPos = row ? y : numb;
            int xPos = row ? numb : x;

            //вызываем методы интерфеса для изменения фона кнопок с цифрами (ходов)
            if (activate) mResults.changeButtonBg(yPos, xPos, row, activate);
            else mResults.changeButtonBg(yPos, xPos, row, activate);
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            if (activate) //если только что активировали, то теперь нужно деактивировать старое
                new MyAnimation(y, x, false, row).execute();
            else //теперь, когда завершили деактивацию, дергаем метод завершения анимации
                onAnimationFinished();
        }

        //наш метод для задержки
        private void sleep(int time) {
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    protected void onResult() {
        //метод интерфеса для отображения результатов
        mResults.onResult(playerOnePoints, playerTwoPoints);
    }

    //Интерфейс для MainActivity, который будет изменять ui элементы
    //*********************************************************************************
    public interface ResultsCallback {

        //для изменения ваших очков и очков соперника
        void changeLabel(boolean upLabel, int points);

        //для изменения цвета кнопок
        void changeButtonBg(int y, int x, boolean row, boolean active);

        //для заполнения кнопок цифрами
        void setButtonText(int y, int x, int text);

        //для блокировки/разблокировки кнопок
        void changeButtonClickable(int y, int x, boolean clickable);

        //по окончанию партии
        void onResult(int one, int two);

        //по нажатию на кнопку
        void onClick(int y, int x, boolean flyDown);
    }
}
