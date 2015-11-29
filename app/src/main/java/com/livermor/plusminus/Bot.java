package com.livermor.plusminus; //не забудьте заменить "livermor" на ваш Company Domain

public class Bot {

    protected int[][] mMatrix; //digits for buttons
    protected boolean[][] mAllowedMoves; //ходы, куда еще не сходили
    protected int mSize; //размер матрицы
    protected int mPlayerPoints = 0, mAiPoints = 0; //очки игроков
    protected boolean mIsVertical; //играем за строки или ряды
    protected int mCurrentActiveNumb; //номер последнего хода (от 0 до размера матрицы(mSize))

    //рейтинги для ходов
    private final static int CANT_GO_THERE = -1000; //если нет хода, то ставим ретинг -1000
    private final static int WORST_MOVE = -500; // ход, когда мы неизбежно проигрываем
    private final static int VICTORY_MOVE = 500; // ход, когда мы неизбежно выигрываем
    private final static int JACKPOT_INCREASE = 9; //надбавка к рейтингу, если ход принесет куш
    private static final int GOOD_ADVANTAGE = 6;//Куш (джекпот), равный разнице в 6 очков или больше

    int depth = 3; //по умолчанию просчитываем на 3 хода вперед

    public Bot(
            int[][] matrix,
            boolean vertical
    ) {
        mMatrix = matrix;
        mSize = matrix.length;
        mIsVertical = vertical;
    }

    //функция, возвращающая номер хода
    public int move(
            int playerPoints,
            int botPoints,
            boolean[][] moves,
            int activeNumb
    ) {
        mPlayerPoints = playerPoints;
        mAiPoints = botPoints;
        mCurrentActiveNumb = activeNumb;
        mAllowedMoves = moves;

        return calcMove();
    }

    //можем задать другую глубину просчета
    public void setDepth(int depth) {
        this.depth = depth;
    }

    protected int calcMove() {
        //функция для определения лучшего хода игрока
        return calcBestMove(depth, mAllowedMoves,
                mCurrentActiveNumb, mIsVertical, mAiPoints, mPlayerPoints);
    }

    private int calcBestMove(int depth, boolean[][] moves, int lastMove, boolean isVert,
                             int myPoints, int hisPoints) {

        int result = mSize; //возвращаем размер матрицы, если нет доступных ходов
        int[] moveRatings = new int[mSize]; //будем хранить рейтинги ходов в массиве

        //если последний ход, возвращаем максимум в ряду (строке)
        if (depth == 1) return findMaxInRow(lastMove, isVert);
        else {

            int yMe, xMe; // координаты ходов текущего игрока
            int yHe, xHe; // координаты ходов оппонента

            for (int i = 0; i < mSize; i++) {

                //если игрок ходит вертикально, то ходим по строкам (i) в ряду (lastMove)
                yMe = isVert ? i : lastMove;
                xMe = isVert ? lastMove : i;

                //если нет хода, ставим ходу минимальный рейтинг
                if (!mAllowedMoves[yMe][xMe]) {
                    moveRatings[i] = CANT_GO_THERE;
                    continue; //переходим к следующему циклу
                }

                int myNewP = myPoints + mMatrix[yMe][xMe];//считаем новые очки игрока
                moves[yMe][xMe] = false;//временно запрещаем ходить туда, куда мы сходили

                //считаем лучший ход для соперника
                int hisBestMove = calcBestMove(depth - 1, moves, i, !isVert, hisPoints, myPoints);

                //если случилось так, что у соперника нет ходов (т.е. вернулся размер матрицы), то..
                if (hisBestMove == mSize) {
                    if (myNewP > hisPoints) //если у меня больше очков, то это победный ход
                        moveRatings[i] = VICTORY_MOVE;
                    else //если меньше, то это ужасный ход
                        moveRatings[i] = WORST_MOVE;

                    moves[yMe][xMe] = true;//Просчеты завершены, возвращаем ходы как было
                    continue;
                }

                //теперь определим ход соперника, для того чтобы посчитать разницу между ходами
                yHe = isVert ? i : hisBestMove;
                xHe = isVert ? hisBestMove : i;
                int hisNewP = hisPoints + mMatrix[yHe][xHe];
                moveRatings[i] = myNewP - hisNewP;

                //и наконец сделаем надбавку к рейтингам ходов в случае, если можно сорвать куш
                //если глубина уже равна 1, то нет смысла делать рассчеты второй раз
                if (depth - 1 != 1) {

                    //на этот раз нам хватит формулы поиска максимума
                    hisBestMove = findMaxInRow(i, !isVert);
                    yHe = isVert ? i : hisBestMove;
                    xHe = isVert ? hisBestMove : i;
                    hisNewP = hisPoints + mMatrix[yHe][xHe];

                    int jackpot = myNewP - hisNewP;//считаем разницу для проверки ситуации куша
                    if (jackpot >= GOOD_ADVANTAGE) { //если куш, то делаем надбавку
                        moveRatings[i] = moveRatings[i] + JACKPOT_INCREASE;
                    }
                }

                moves[yMe][xMe] = true;//Просчеты завершены, возвращаем ходы как было

            } // рейтинги ходов проставлены, пора выбирать ход с макс. рейтингом

            //начинаем с предположения, что максимум — это самый худший вариант (ходов вообще нет)
            int max = CANT_GO_THERE;
            for (int i = 0; i < mSize; i++) {
                if (moveRatings[i] > max) {
                    max = moveRatings[i];//если есть ход лучше, пусть теперь он будет максимумом
                    result = i;
                }
            }
        }

        //возвращаем ход с максимальным рейтингом
        return result;
    }

    //возвращает ход, соответствующий максимальному числу в указанном ряду(строке)
    private int findMaxInRow(int lastM, boolean isVert) {

        int currentMax = -10;
        int move = mSize;

        int y = 0, x = 0;
        for (int i = 0; i < mSize; i++) {
            y = isVert ? i : lastM;
            x = isVert ? lastM : i;
            int temp = mMatrix[y][x];
            if (mAllowedMoves[y][x] && currentMax <= temp) {
                currentMax = temp;
                move = i;
            }
        }

        return move;
    }
}
