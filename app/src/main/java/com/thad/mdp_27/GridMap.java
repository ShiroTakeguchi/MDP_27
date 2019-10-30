package com.thad.mdp_27;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class GridMap extends View {

    private static final String TAG = "GridMap";
    private static final int COL = 15, ROW = 20;
    private static float cellSize;
    private static JSONObject receivedJsonObject = new JSONObject();
    private static JSONObject mapInformation;
    private static JSONObject backupMapInformation;
    private static Cell[][] cells;
    private static String robotDirection = "None";
    private static int[] startCoord = new int[]{-1, -1};
    private static int[] curCoord = new int[]{-1, -1};
    private static int[] oldCoord = new int[]{-1, -1};
    private static int[] waypointCoord = new int[]{-1, -1};
    private static ArrayList<String[]> arrowCoord = new ArrayList<>();
    private static ArrayList<int[]> obstacleCoord = new ArrayList<>();
    private static boolean autoUpdate = false;
    private static boolean mapDrawn = false;
    private static boolean canDrawRobot = false;
    private static boolean setWaypointStatus = false;
    private static boolean startCoordStatus = false;
    private static boolean setObstacleStatus = false;
    private static boolean unSetCellStatus = false;
    private static boolean setExploredStatus = false;
    private static boolean validPosition = false;
    private Bitmap arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_error);

    private Paint blackPaint = new Paint();
    private Paint obstacleColor = new Paint();
    private Paint robotColor = new Paint();
    private Paint endColor = new Paint();
    private Paint startColor = new Paint();
    private Paint waypointColor = new Paint();
    private Paint unexploredColor = new Paint();
    private Paint exploredColor = new Paint();
    private Paint arrowColor = new Paint();
    private Paint fastestPathColor = new Paint();

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    public GridMap(Context context) {
        super(context);
        init(null);
    }

    public GridMap(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
        blackPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        obstacleColor.setColor(Color.BLACK);
        robotColor.setColor(Color.GREEN);
        endColor.setColor(Color.RED);
        startColor.setColor(Color.CYAN);
        waypointColor.setColor(Color.YELLOW);
        unexploredColor.setColor(Color.GRAY);
        exploredColor.setColor(Color.WHITE);
        arrowColor.setColor(Color.BLACK);
        fastestPathColor.setColor(Color.MAGENTA);
    }

    private void init(@Nullable AttributeSet attrs) {
        setWillNotDraw(false);
    }

    private int convertRow(int row) {
        return (20 - row);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        showLog("Entering onDraw");
        super.onDraw(canvas);
        showLog("Redrawing map");

        ArrayList<String[]> arrowCoord = this.getArrowCoord();
        int[] curCoord = this.getCurCoord();

        if (!this.getMapDrawn()) {
            canvas.drawColor(Color.parseColor("#B0E2FF"));
            String[] dummyArrowCoord = new String[3];
            dummyArrowCoord[0] = "1";
            dummyArrowCoord[1] = "1";
            dummyArrowCoord[2] = "dummy";
            arrowCoord.add(dummyArrowCoord);
            this.createCell();
            this.setEndCoord(14, 19);
            mapDrawn = true;
        }

        this.drawIndividualCell(canvas);
        this.drawGridNumber(canvas);
        if (this.getCanDrawRobot())
            this.drawRobot(canvas, curCoord);
        this.drawArrow(canvas, arrowCoord);

        showLog("Exiting onDraw");
    }

    private void createCell() {
        showLog("Entering cellCreate");
        cells = new Cell[COL +1][ROW + 1];
        this.calculateDimension();
        cellSize = this.getCellSize();

        for (int x = 0; x <= COL; x++)
            for (int y = 0; y <= ROW; y++)
                cells[x][y] = new Cell(x * cellSize + (cellSize / 30), y * cellSize + (cellSize / 30), (x + 1) * cellSize, (y + 1) * cellSize, unexploredColor, "unexplored");
        showLog("Exiting createCell");
    }

    public void setAutoUpdate(boolean autoUpdate) throws JSONException {
        showLog(String.valueOf(backupMapInformation));
        if (!autoUpdate)
            backupMapInformation = this.getReceivedJsonObject();
        else {
                setReceivedJsonObject(backupMapInformation);
                backupMapInformation = null;
                this.updateMapInformation();
        }
        GridMap.autoUpdate = autoUpdate;
    }

    public boolean getAutoUpdate() {
        return autoUpdate;
    }

    public boolean getMapDrawn() {
        return mapDrawn;
    }

    private void setValidPosition(boolean status) {
        validPosition = status;
    }

    public boolean getValidPosition() {
        return validPosition;
    }

    public void setUnSetCellStatus(boolean status) {
        unSetCellStatus = status;
    }

    public boolean getUnSetCellStatus() {
        return unSetCellStatus;
    }

    public void setSetObstacleStatus(boolean status) {
        setObstacleStatus = status;
    }

    public boolean getSetObstacleStatus() {
        return setObstacleStatus;
    }

    public void setExploredStatus(boolean status) {
        setExploredStatus = status;
    }

    public boolean getExploredStatus() {
        return setExploredStatus;
    }

    public void setStartCoordStatus(boolean status) {
        startCoordStatus = status;
    }

    private boolean getStartCoordStatus() {
        return startCoordStatus;
    }

    public void setWaypointStatus(boolean status) {
        setWaypointStatus = status;
    }

    public boolean getCanDrawRobot() {
        return canDrawRobot;
    }

    public void setEndCoord(int col, int row) {
        showLog("Entering setEndCoord");
        row = this.convertRow(row);
        for (int x = col - 1; x <= col + 1; x++)
            for (int y = row - 1; y <= row + 1; y++)
                cells[x][y].setType("end");
        showLog("Exiting setEndCoord");
    }

    public void setStartCoord(int col, int row) {
        showLog("Entering setStartCoord");
        startCoord[0] = col;
        startCoord[1] = row;

        if (this.getStartCoordStatus())
            this.setCurCoord(col, row, "up");
        showLog("Exiting setStartCoord");
    }

    private int[] getStartCoord() {
        return startCoord;
    }

    public void setCurCoord(int col, int row, String direction) {
        showLog("Entering setCurCoord");
        curCoord[0] = col;
        curCoord[1] = row;
        this.setRobotDirection(direction);
        this.updateRobotAxis(col, row, direction);

        row = this.convertRow(row);
        for (int x = col - 1; x <= col + 1; x++)
            for (int y = row - 1; y <= row + 1; y++)
                cells[x][y].setType("robot");
        showLog("Exiting setCurCoord");
    }

    private void updateRobotAxis(int col, int row, String direction) {
        TextView xAxisTextView =  ((Activity)this.getContext()).findViewById(R.id.xAxisTextView);
        TextView yAxisTextView =  ((Activity)this.getContext()).findViewById(R.id.yAxisTextView);
        TextView directionAxisTextView =  ((Activity)this.getContext()).findViewById(R.id.directionAxisTextView);

        xAxisTextView.setText(String.valueOf(col-1));
        yAxisTextView.setText(String.valueOf(row-1));
        directionAxisTextView.setText(direction);
    }

    public int[] getCurCoord() {
        return curCoord;
    }

    public void setRobotDirection(String direction) {
        this.sharedPreferences();
        robotDirection = direction;
        editor.putString("direction", direction);
        editor.commit();
        this.invalidate();
    }

    public String getRobotDirection() {
        return robotDirection;
    }

    private void setWaypointCoord(int col, int row) throws JSONException {
        showLog("Entering setWaypointCoord");
        waypointCoord[0] = col-1;
        waypointCoord[1] = row-1;

        row = this.convertRow(row);
        cells[col][row].setType("waypoint");

        //MainActivity.printMessage("waypoint", waypointCoord[0], waypointCoord[1]);
        MainActivity.printMessage("XWAYPOINT " + waypointCoord[0] + "," + waypointCoord[1]);
        showLog("Exiting setWaypointCoord");
    }

    private int[] getWaypointCoord() {
        return waypointCoord;
    }

    private void setObstacleCoord(int col, int row) {
        showLog("Entering setObstacleCoord");
        int[] obstacleCoord = new int[]{col, row};
        GridMap.obstacleCoord.add(obstacleCoord);
        row = this.convertRow(row);
        cells[col][row].setType("obstacle");
        showLog("Exiting setObstacleCoord");
    }

    private ArrayList<int[]> getObstacleCoord() {
        return obstacleCoord;
    }

    public void moveRobot(String direction) {
        showLog("Entering moveRobot");
        setValidPosition(false);
        int[] curCoord = this.getCurCoord();
        ArrayList<int[]> obstacleCoord = this.getObstacleCoord();
        this.setOldRobotCoord(curCoord[0], curCoord[1]);
        int[] oldCoord = this.getOldRobotCoord();
        String robotDirection = getRobotDirection();
        String backupDirection = robotDirection;

        switch (robotDirection) {
            case "up":
                switch (direction) {
                    case "forward":
                        if (curCoord[1] != 19) {
                            curCoord[1] += 1;
                            validPosition = true;
                        }
                        break;
                    case "right":
                        robotDirection = "right";
                        break;
                    case "back":
                        if (curCoord[1] != 2) {
                            curCoord[1] -= 1;
                            validPosition = true;
                        }
                        break;
                    case "left":
                        robotDirection = "left";
                        break;
                    default:
                        robotDirection = "error up";
                        break;
                }
                break;
            case "right":
                switch (direction) {
                    case "forward":
                        if (curCoord[0] != 14) {
                            curCoord[0] += 1;
                            validPosition = true;
                        }
                        break;
                    case "right":
                        robotDirection = "down";
                        break;
                    case "back":
                        if (curCoord[0] != 2) {
                            curCoord[0] -= 1;
                            validPosition = true;
                        }
                        break;
                    case "left":
                        robotDirection = "up";
                        break;
                    default:
                        robotDirection = "error right";
                }
                break;
            case "down":
                switch (direction) {
                    case "forward":
                        if (curCoord[1] != 2) {
                            curCoord[1] -= 1;
                            validPosition = true;
                        }
                        break;
                    case "right":
                        robotDirection = "left";
                        break;
                    case "back":
                        if (curCoord[1] != 19) {
                            curCoord[1] += 1;
                            validPosition = true;
                        }
                        break;
                    case "left":
                        robotDirection = "right";
                        break;
                    default:
                        robotDirection = "error down";
                }
                break;
            case "left":
                switch (direction) {
                    case "forward":
                        if (curCoord[0] != 2) {
                            curCoord[0] -= 1;
                            validPosition = true;
                        }
                        break;
                    case "right":
                        robotDirection = "up";
                        break;
                    case "back":
                        if (curCoord[0] != 14) {
                            curCoord[0] += 1;
                            validPosition = true;
                        }
                        break;
                    case "left":
                        robotDirection = "down";
                        break;
                    default:
                        robotDirection = "error left";
                }
                break;
            default:
                robotDirection = "error moveCurCoord";
                break;
        }
        if (getValidPosition())
            for (int x = curCoord[0] - 1; x <= curCoord[0] + 1; x++) {
                for (int y = curCoord[1] - 1; y <= curCoord[1] + 1; y++) {
                    for (int i = 0; i < obstacleCoord.size(); i++) {
                        if (obstacleCoord.get(i)[0] != x || obstacleCoord.get(i)[1] != y)
                            setValidPosition(true);
                        else {
                            setValidPosition(false);
                            break;
                        }
                    }
                    if (!getValidPosition())
                        break;
                }
                if (!getValidPosition())
                    break;
            }
        if (getValidPosition())
            this.setCurCoord(curCoord[0], curCoord[1], robotDirection);
        else {
            if (direction.equals("forward") || direction.equals("back"))
                robotDirection = backupDirection;
            this.setCurCoord(oldCoord[0], oldCoord[1], robotDirection);
        }
        this.invalidate();
        showLog("Exiting moveRobot");
    }

    private void setOldRobotCoord(int oldCol, int oldRow) {
        showLog("Entering setOldRobotCoord");
        oldCoord[0] = oldCol;
        oldCoord[1] = oldRow;
        oldRow = this.convertRow(oldRow);
        for (int x = oldCol - 1; x <= oldCol + 1; x++)
            for (int y = oldRow - 1; y <= oldRow + 1; y++)
                cells[x][y].setType("explored");
        showLog("Exiting setOldRobotCoord");
    }

    private int[] getOldRobotCoord() {
        return oldCoord;
    }

    private void setArrowCoordinate(int col, int row, String arrowDirection) {
        showLog("Entering setArrowCoordinate");
        int[] obstacleCoord = new int[]{col, row};
        this.getObstacleCoord().add(obstacleCoord);
        String[] arrowCoord = new String[3];
        arrowCoord[0] = String.valueOf(col);
        arrowCoord[1] = String.valueOf(row);
        arrowCoord[2] = arrowDirection;
        this.getArrowCoord().add(arrowCoord);

        row = convertRow(row);
        cells[col][row].setType("arrow");
        showLog("Exiting setArrowCoordinate");
    }

    private ArrayList<String[]> getArrowCoord() {
        return arrowCoord;
    }

    private void drawIndividualCell(Canvas canvas) {
        showLog("Entering drawIndividualCell");
        for (int x = 1; x <= COL; x++)
            for (int y = 0; y < ROW; y++)
                for (int i = 0; i < this.getArrowCoord().size(); i++)
                    canvas.drawRect(cells[x][y].startX, cells[x][y].startY, cells[x][y].endX, cells[x][y].endY, cells[x][y].paint);

        showLog("Exiting drawIndividualCell");
    }

    private void drawGridNumber(Canvas canvas) {
        showLog("Entering drawGridNumber");
        for (int x = 1; x <= COL; x++) {
            if (x > 9)
                canvas.drawText(Integer.toString(x-1), cells[x][20].startX + (cellSize / 5), cells[x][20].startY + (cellSize / 3), blackPaint);
            else
                canvas.drawText(Integer.toString(x-1), cells[x][20].startX + (cellSize / 3), cells[x][20].startY + (cellSize / 3), blackPaint);
        }
        for (int y = 0; y < ROW; y++) {
            if ((19 - y) > 9)
                canvas.drawText(Integer.toString(19 - y), cells[0][y].startX + (cellSize / 2), cells[0][y].startY + (cellSize / 1.5f), blackPaint);
            else
                canvas.drawText(Integer.toString(19 - y), cells[0][y].startX + (cellSize / 1.5f), cells[0][y].startY + (cellSize / 1.5f), blackPaint);
        }
        showLog("Exiting drawGridNumber");
    }

    private void drawRobot(Canvas canvas, int[] curCoord) {
        showLog("Entering drawRobot");
        int androidRowCoord = this.convertRow(curCoord[1]);
        for (int y = androidRowCoord; y <= androidRowCoord + 1; y++)
            canvas.drawLine(cells[curCoord[0] - 1][y].startX, cells[curCoord[0] - 1][y].startY - (cellSize / 30), cells[curCoord[0] + 1][y].endX, cells[curCoord[0] + 1][y].startY - (cellSize / 30), robotColor);
        for (int x = curCoord[0] - 1; x < curCoord[0] + 1; x++)
            canvas.drawLine(cells[x][androidRowCoord - 1].startX - (cellSize / 30) + cellSize, cells[x][androidRowCoord - 1].startY, cells[x][androidRowCoord + 1].startX - (cellSize / 30) + cellSize, cells[x][androidRowCoord + 1].endY, robotColor);

        switch (this.getRobotDirection()) {
            case "up":
                canvas.drawLine(cells[curCoord[0] - 1][androidRowCoord + 1].startX, cells[curCoord[0] - 1][androidRowCoord + 1].endY, (cells[curCoord[0]][androidRowCoord - 1].startX + cells[curCoord[0]][androidRowCoord - 1].endX) / 2, cells[curCoord[0]][androidRowCoord - 1].startY, blackPaint);
                canvas.drawLine((cells[curCoord[0]][androidRowCoord - 1].startX + cells[curCoord[0]][androidRowCoord - 1].endX) / 2, cells[curCoord[0]][androidRowCoord - 1].startY, cells[curCoord[0] + 1][androidRowCoord + 1].endX, cells[curCoord[0] + 1][androidRowCoord + 1].endY, blackPaint);
                break;
            case "down":
                canvas.drawLine(cells[curCoord[0] - 1][androidRowCoord - 1].startX, cells[curCoord[0] - 1][androidRowCoord - 1].startY, (cells[curCoord[0]][androidRowCoord + 1].startX + cells[curCoord[0]][androidRowCoord + 1].endX) / 2, cells[curCoord[0]][androidRowCoord + 1].endY, blackPaint);
                canvas.drawLine((cells[curCoord[0]][androidRowCoord + 1].startX + cells[curCoord[0]][androidRowCoord + 1].endX) / 2, cells[curCoord[0]][androidRowCoord + 1].endY, cells[curCoord[0] + 1][androidRowCoord - 1].endX, cells[curCoord[0] + 1][androidRowCoord - 1].startY, blackPaint);
                break;
            case "right":
                canvas.drawLine(cells[curCoord[0] - 1][androidRowCoord - 1].startX, cells[curCoord[0] - 1][androidRowCoord - 1].startY, cells[curCoord[0] + 1][androidRowCoord].endX, cells[curCoord[0] + 1][androidRowCoord - 1].endY + (cells[curCoord[0] + 1][androidRowCoord].endY - cells[curCoord[0] + 1][androidRowCoord - 1].endY) / 2, blackPaint);
                canvas.drawLine(cells[curCoord[0] + 1][androidRowCoord].endX, cells[curCoord[0] + 1][androidRowCoord - 1].endY + (cells[curCoord[0] + 1][androidRowCoord].endY - cells[curCoord[0] + 1][androidRowCoord - 1].endY) / 2, cells[curCoord[0] - 1][androidRowCoord + 1].startX, cells[curCoord[0] - 1][androidRowCoord + 1].endY, blackPaint);
                break;
            case "left":
                canvas.drawLine(cells[curCoord[0] + 1][androidRowCoord - 1].endX, cells[curCoord[0] + 1][androidRowCoord - 1].startY, cells[curCoord[0] - 1][androidRowCoord].startX, cells[curCoord[0] - 1][androidRowCoord - 1].endY + (cells[curCoord[0] - 1][androidRowCoord].endY - cells[curCoord[0] - 1][androidRowCoord - 1].endY) / 2, blackPaint);
                canvas.drawLine(cells[curCoord[0] - 1][androidRowCoord].startX, cells[curCoord[0] - 1][androidRowCoord - 1].endY + (cells[curCoord[0] - 1][androidRowCoord].endY - cells[curCoord[0] - 1][androidRowCoord - 1].endY) / 2, cells[curCoord[0] + 1][androidRowCoord + 1].endX, cells[curCoord[0] + 1][androidRowCoord + 1].endY, blackPaint);
                break;
            default:
                Toast.makeText(this.getContext(), "Error with drawing robot (unknown direction)", Toast.LENGTH_LONG).show();
                break;
        }
        showLog("Exiting drawRobot");
    }

    private void drawArrow(Canvas canvas, ArrayList<String[]> arrowCoord) {
        showLog("Entering drawArrow");
        RectF rect;

        for (int i = 0; i < arrowCoord.size(); i++) {
            if (!arrowCoord.get(i)[2].equals("dummy")) {
                int col = Integer.parseInt(arrowCoord.get(i)[0]);
                int row = convertRow(Integer.parseInt(arrowCoord.get(i)[1]));
                rect = new RectF(col * cellSize, row * cellSize, (col + 1) * cellSize, (row + 1) * cellSize);
                switch (arrowCoord.get(i)[2]) {
                    case "1":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_white_arrow_up);
                        break;
                    case "2":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_red_arrow_down);
                        break;
                    case "3":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_green_arrow_right);
                        break;
                    case "4":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_blue_arrow_left);
                        break;
                    case "5":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_yellow_dot);
                        break;
                    case "6":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_blue_1);
                        break;
                    case "7":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_green_2);
                        break;
                    case "8":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_red_3);
                        break;
                    case "9":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_white_4);
                        break;
                    case "10":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_yellow_5);
                        break;
                    case "11":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_red_a);
                        break;
                    case "12":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_green_b);
                        break;
                    case "13":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_white_c);
                        break;
                    case "14":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_blue_d);
                        break;
                    case "15":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_yellow_e);
                        break;
                    default:
                        break;
                }
                canvas.drawBitmap(arrowBitmap, null, rect, null);
            }
            showLog("Exiting drawArrow");
        }
    }

    private class Cell {
        float startX, startY, endX, endY;
        Paint paint;
        String type;

        private Cell(float startX, float startY, float endX, float endY, Paint paint, String type) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.paint = paint;
            this.type = type;
        }

        public void setType(String type) {
            this.type = type;
            switch (type) {
                case "obstacle":
                    this.paint = obstacleColor;
                    break;
                case "robot":
                    this.paint = robotColor;
                    break;
                case "end":
                    this.paint = endColor;
                    break;
                case "start":
                    this.paint = startColor;
                    break;
                case "waypoint":
                    this.paint = waypointColor;
                    break;
                case "unexplored":
                    this.paint = unexploredColor;
                    break;
                case "explored":
                    this.paint = exploredColor;
                    break;
                case "arrow":
                    this.paint = arrowColor;
                    break;
                case "fastestPath":
                    this.paint = fastestPathColor;
                    break;
                default:
                    showLog("setTtype default: " + type);
                    break;
            }
        }
    }

    private void calculateDimension() {
        this.setCellSize(getWidth()/(COL+1));
    }

    private void setCellSize(float cellSize) {
        GridMap.cellSize = cellSize;
    }

    private float getCellSize() {
        return cellSize;
    }

    public void refreshMap() {
        if (this.getAutoUpdate())

            postInvalidateDelayed(500);
    }

    private String parseHexCharToBinary(String hexStr)
    {
        String fullString = "";
        for (int i = 0; i < hexStr.length(); i++)
        {
            String hexChar = Character.toString(hexStr.charAt(i));
            int hexValue = 0;
            try
            {
                hexValue = Integer.parseInt(hexChar, 16);
            }
            catch (NumberFormatException e)
            {
                Log.e("ParseHexChar", e.getMessage());
            }
            String binary = String.format("%4s", Integer.toString(hexValue, 2)).replace(' ', '0');
            fullString += binary;
        }
        return fullString;
    }

    public void updateMapInformation() throws JSONException {
        showLog("Entering updateMapInformation");
        JSONObject mapInformation = this.getReceivedJsonObject();
        showLog("updateMapInformation --- mapInformation: " + mapInformation);
        JSONArray infoJsonArray;
        JSONObject infoJsonObject;
        String hexStringExplored, hexStringObstacle, exploredString, obstacleString;
        BigInteger hexBigIntegerExplored, hexBigIntegerObstacle;
        String message;

        if (mapInformation == null) {
            refreshMap();
            showLog("Map Refreshing");
            MainActivity.printMessage("XY");
            return;
        }
        for(int i=0; i<mapInformation.names().length(); i++) {
            message = "updateMapInformation Default message";
            switch (mapInformation.names().getString(i)) {
                case "map":
                    MainActivity.printMessage("XY");
                    infoJsonArray = mapInformation.getJSONArray("map");
                    infoJsonObject = infoJsonArray.getJSONObject(0);

                    hexStringExplored = infoJsonObject.getString("explored");
                    hexBigIntegerExplored = new BigInteger(hexStringExplored, 16);
                    exploredString = hexBigIntegerExplored.toString(2);
                    showLog("updateMapInformation.exploredString: " + exploredString);

                    int x, y;
                    for (int j=0; j<exploredString.length()-4; j++) {
                        y = 19 - (j/15);
                        x = 1 + j - ((19-y)*15);
                        if ((String.valueOf(exploredString.charAt(j+2))).equals("1") && !cells[x][y].type.equals("robot"))
                            cells[x][y].setType("explored");
                        else if ((String.valueOf(exploredString.charAt(j+2))).equals("0") && !cells[x][y].type.equals("robot"))
                            cells[x][y].setType("unexplored");
                    }

                    int length = infoJsonObject.getInt("length");
                    if (length%4 == 0)
                        length = length + 4;
                    while (length%4 != 0)
                            length ++;

                    hexStringObstacle = infoJsonObject.getString("obstacle");
                    showLog("updateMapInformation hexStringObstacle: " + hexStringObstacle);
                    hexBigIntegerObstacle = new BigInteger(hexStringObstacle, 16);
                    showLog("updateMapInformation hexBigIntegerObstacle: " + hexBigIntegerObstacle);
                    obstacleString = hexBigIntegerObstacle.toString(2);
                    while (obstacleString.length() < length) {
                        obstacleString = "0" + obstacleString;
                    }
                    showLog("updateMapInformation obstacleString: " + obstacleString);


                    int k = 0;
                    for (int row = ROW; row >= 0; row--)
                        for (int col = 1; col <= COL; col++)
                            if ((cells[col][row].type.equals("explored")||(cells[col][row].type.equals("robot"))) && k < obstacleString.length()) {
                                if ((String.valueOf(obstacleString.charAt(k))).equals("1"))
                                    this.setObstacleCoord(col, 20 - row);
                                k++;
                            }
                    //MainActivity.printMessage("XY");
                    int[] waypointCoord = this.getWaypointCoord();
                    if (waypointCoord[0] >= 0 && waypointCoord[1] >= 0)
                        cells[waypointCoord[0]+1][20-waypointCoord[1]-1].setType("waypoint");
                    break;
                case "robot":
                    MainActivity.printMessage("XY");
                    if (canDrawRobot)
                        this.setOldRobotCoord(curCoord[0], curCoord[1]);
                    infoJsonArray = mapInformation.getJSONArray("robot");
                    infoJsonObject = infoJsonArray.getJSONObject(0);

                    /*for (int row = ROW-1; row >= 0; row--)
                        for (int col = 1; col <= COL; col++)
                            cells[col][row].setType("unexplored");
                    */

                    this.setStartCoord(infoJsonObject.getInt("x"), infoJsonObject.getInt("y"));
                    this.setCurCoord(infoJsonObject.getInt("x"), infoJsonObject.getInt("y"), infoJsonObject.getString("direction"));
                    canDrawRobot = true;
                    //MainActivity.printMessage("XY");
                    break;
                case "waypoint":
                    infoJsonArray = mapInformation.getJSONArray("waypoint");
                    infoJsonObject = infoJsonArray.getJSONObject(0);
                    this.setWaypointCoord(infoJsonObject.getInt("x"), infoJsonObject.getInt("y"));
                    setWaypointStatus = true;
                    break;
                case "obstacle":
                    infoJsonArray = mapInformation.getJSONArray("obstacle");
                    for (int j = 0; j < infoJsonArray.length(); j++) {
                        infoJsonObject = infoJsonArray.getJSONObject(j);
                        this.setObstacleCoord(infoJsonObject.getInt("x"), infoJsonObject.getInt("y"));
                    }
                    message = "No. of Obstacle: " + String.valueOf(infoJsonArray.length());
                    break;
                case "arrow":
                    infoJsonArray = mapInformation.getJSONArray("arrow");
                    for (int j = 0; j < infoJsonArray.length(); j++) {
                        infoJsonObject = infoJsonArray.getJSONObject(j);
                        if (!infoJsonObject.getString("face").equals("dummy")) {
                            this.setArrowCoordinate(infoJsonObject.getInt("x")+1, infoJsonObject.getInt("y")+1, infoJsonObject.getString("face"));
                            message = "Arrow:  (" + String.valueOf(infoJsonObject.getInt("x")) + "," + String.valueOf(infoJsonObject.getInt("y")) + "), face: " + infoJsonObject.getString("face");
                        }
                    }
                    break;
                case "move":
                    infoJsonArray = mapInformation.getJSONArray("move");
                    infoJsonObject = infoJsonArray.getJSONObject(0);
                    if (canDrawRobot)
                        moveRobot(infoJsonObject.getString("direction"));
                    message = "moveDirection: " + infoJsonObject.getString("direction");
                    break;
                case "status":
                    infoJsonArray = mapInformation.getJSONArray("status");
                    infoJsonObject = infoJsonArray.getJSONObject(0);
                    printRobotStatus(infoJsonObject.getString("status"));
                    message = "status: " + infoJsonObject.getString("status");
                    break;
                default:
                    message = "Unintended default for JSONObject";
                    break;
            }
            if (!message.equals("updateMapInformation Default message"))
                MainActivity.receiveMessage(message);
        }
        showLog("Exiting updateMapInformation");
        this.invalidate();
    }

    public void setReceivedJsonObject(JSONObject receivedJsonObject) {
        showLog("Entered setReceivedJsonObject");
        GridMap.receivedJsonObject = receivedJsonObject;
        backupMapInformation = receivedJsonObject;
    }

    public JSONObject getReceivedJsonObject() {
        return receivedJsonObject;
    }

    public JSONObject getMapInformation() {
        showLog("getCreateJsonObject() :" + getCreateJsonObject());
        return this.getCreateJsonObject();}

    public void printRobotStatus(String message) {
        TextView robotStatusTextView = ((Activity)this.getContext()).findViewById(R.id.robotStatusTextView);
        robotStatusTextView.setText(message);
    }

        @Override
    public boolean onTouchEvent(MotionEvent event) {
        showLog("Entering onTouchEvent");

        if (event.getAction() == MotionEvent.ACTION_DOWN && this.getAutoUpdate() == false) {
            int column = (int) (event.getX() / cellSize);
            int row = this.convertRow((int) (event.getY() / cellSize));
            ToggleButton setStartPointToggleBtn = ((Activity)this.getContext()).findViewById(R.id.setStartPointToggleBtn);
            ToggleButton setWaypointToggleBtn = ((Activity)this. getContext()).findViewById(R.id.setWaypointToggleBtn);

            if (startCoordStatus) {
                if (canDrawRobot) {
                    int[] startCoord = this.getStartCoord();
                    if (startCoord[0] >= 2 && startCoord[1] >= 2) {
                        startCoord[1] = this.convertRow(startCoord[1]);
                        for (int x = startCoord[0] - 1; x <= startCoord[0] + 1; x++)
                            for (int y = startCoord[1] - 1; y <= startCoord[1] + 1; y++)
                                cells[x][y].setType("unexplored");
                            showLog("first one");
                    }
                }
                else
                    canDrawRobot = true;
                this.setStartCoord(column, row);
                startCoordStatus = false;
                try {
                    MainActivity.printMessage("starting", column, row);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                updateRobotAxis(column, row, "up");
                if (setStartPointToggleBtn.isChecked())
                    setStartPointToggleBtn.toggle();
                showLog("maybe this ran");
                this.invalidate();
                return true;
            }
            if (setWaypointStatus) {
               int[] waypointCoord = this.getWaypointCoord();
               if (waypointCoord[0] >= 1 && waypointCoord[1] >= 1)
                   cells[waypointCoord[0]][this.convertRow(waypointCoord[1])].setType("unexplored");
                setWaypointStatus = false;
                try {
                   this.setWaypointCoord(column, row);
               } catch (JSONException e) {
                   e.printStackTrace();
               }
                if (setWaypointToggleBtn.isChecked())
                   setWaypointToggleBtn.toggle();
               this.invalidate();
               return true;
            }
            if (setObstacleStatus) {
                this.setObstacleCoord(column, row);
                this.invalidate();
                return true;
            }
            if (setExploredStatus) {
                cells[column][20-row].setType("explored");
                this.invalidate();
                return true;
            }
            if (unSetCellStatus) {
                ArrayList<int[]> obstacleCoord = this.getObstacleCoord();
                cells[column][20-row].setType("unexplored");
                for (int i=0; i<obstacleCoord.size(); i++)
                    if (obstacleCoord.get(i)[0] == column && obstacleCoord.get(i)[1] == row)
                        obstacleCoord.remove(i);
                this.invalidate();
                showLog("it ran");
                return true;
            }
        }
        showLog("Exiting onTouchEvent");
        return false;
    }

    public void toggleCheckedBtn(String buttonName) {
        ToggleButton setStartPointToggleBtn = ((Activity)this.getContext()).findViewById(R.id.setStartPointToggleBtn);
        ToggleButton setWaypointToggleBtn = ((Activity)this.getContext()).findViewById(R.id.setWaypointToggleBtn);
        ImageButton obstacleImageBtn = ((Activity)this.getContext()).findViewById(R.id.obstacleImageBtn);
        ImageButton exploredImageBtn = ((Activity)this.getContext()).findViewById(R.id.exploredImageBtn);
        ImageButton clearImageBtn = ((Activity)this. getContext()).findViewById(R.id.clearImageBtn);

        if (!buttonName.equals("setStartPointToggleBtn"))
            if (setStartPointToggleBtn.isChecked()) {
                this.setStartCoordStatus(false);
                setStartPointToggleBtn.toggle();
            }
        if (!buttonName.equals("setWaypointToggleBtn"))
            if (setWaypointToggleBtn.isChecked()) {
                this.setWaypointStatus(false);
                setWaypointToggleBtn.toggle();
            }
        if (!buttonName.equals("exploredImageBtn"))
            if (exploredImageBtn.isEnabled())
                this.setExploredStatus(false);
        if (!buttonName.equals("obstacleImageBtn"))
            if (obstacleImageBtn.isEnabled())
                this.setSetObstacleStatus(false);
        if (!buttonName.equals("clearImageBtn"))
            if (clearImageBtn.isEnabled())
                this.setUnSetCellStatus(false);
    }

    public JSONObject getCreateJsonObject() {
        showLog("Entering getCreateJsonObject");
        String exploredString = "11";
        String obstacleString = "";
        String hexStringObstacle = "";
        String hexStringExplored = "";
        BigInteger hexBigIntegerObstacle, hexBigIntegerExplored;
        int[] waypointCoord = this.getWaypointCoord();
        int[] curCoord = this.getCurCoord();
        String robotDirection = this.getRobotDirection();
        List<int[]> obstacleCoord = new ArrayList<>(this.getObstacleCoord());
        List<String[]> arrowCoord = new ArrayList<>(this.getArrowCoord());

        TextView robotStatusTextView = ((Activity) this.getContext()).findViewById(R.id.robotStatusTextView);

        JSONObject map = new JSONObject();
        for (int y = ROW - 1; y >= 0; y--)
            for (int x = 1; x <= COL; x++)
                if (cells[x][y].type.equals("explored") || cells[x][y].type.equals("robot") || cells[x][y].type.equals("obstacle") || cells[x][y].type.equals("arrow"))
                    exploredString = exploredString + "1";
                else
                    exploredString = exploredString + "0";
        exploredString = exploredString + "11";
        showLog("exploredString: " + exploredString);

        hexBigIntegerExplored = new BigInteger(exploredString, 2);
        showLog("hexBigIntegerExplored: " + hexBigIntegerExplored);
        hexStringExplored = hexBigIntegerExplored.toString(16);
        showLog("hexStringExplored: " + hexStringExplored);

        for (int y=ROW-1; y>=0; y--)
            for (int x=1; x<=COL; x++)
                if (cells[x][y].type.equals("explored") || cells[x][y].type.equals("robot"))
                    obstacleString = obstacleString + "0";
                else if (cells[x][y].type.equals("obstacle") || cells[x][y].type.equals("arrow"))
                    obstacleString = obstacleString + "1";
        showLog("Before loop: obstacleString: " + obstacleString + ", length: " + obstacleString.length());

        while ((obstacleString.length() % 8) != 0) {
            obstacleString = obstacleString + "0";
        }

        showLog("After loop: obstacleString: " + obstacleString + ", length: " + obstacleString.length());

        if (!obstacleString.equals("")) {
            hexBigIntegerObstacle = new BigInteger(obstacleString, 2);
            showLog("hexBigIntegerObstacle: " + hexBigIntegerObstacle);
            hexStringObstacle = hexBigIntegerObstacle.toString(16);
            if (hexStringObstacle.length() % 2 != 0)
                hexStringObstacle = "0" + hexStringObstacle;
            showLog("hexStringObstacle: " + hexStringObstacle);
        }
        try {
            map.put("explored", hexStringExplored);
            map.put("length", obstacleString.length());
            if (!obstacleString.equals(""))
                map.put("obstacle", hexStringObstacle);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONArray jsonMap = new JSONArray();
        jsonMap.put(map);

        JSONArray jsonRobot = new JSONArray();
        if (curCoord[0] >= 2 && curCoord[1] >= 2)
            try {
                JSONObject robot = new JSONObject();
                robot.put("x", curCoord[0]);
                robot.put("y", curCoord[1]);
                robot.put("direction", robotDirection);
                jsonRobot.put(robot);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        JSONArray jsonWaypoint = new JSONArray();
        if (waypointCoord[0] >= 1 && waypointCoord[1] >= 1)
            try {
                JSONObject waypoint = new JSONObject();
                waypoint.put("x", waypointCoord[0]);
                waypoint.put("y", waypointCoord[1]);
                setWaypointStatus = true;
                jsonWaypoint.put(waypoint);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        JSONArray jsonObstacle = new JSONArray();
        for (int i=0; i<obstacleCoord.size(); i++)
            try {
                JSONObject obstacle = new JSONObject();
                obstacle.put("x", obstacleCoord.get(i)[0]);
                obstacle.put("y", obstacleCoord.get(i)[1]);
                jsonObstacle.put(obstacle);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        JSONArray jsonArrow = new JSONArray();
        for (int i=0; i<arrowCoord.size(); i++) {
            try {
                JSONObject arrow = new JSONObject();
                arrow.put("x", Integer.parseInt(arrowCoord.get(i)[0]));
                arrow.put("y", Integer.parseInt(arrowCoord.get(i)[1]));
                arrow.put("face", arrowCoord.get(i)[2]);
                jsonArrow.put(arrow);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        JSONArray jsonStatus = new JSONArray();
        try {
            JSONObject status = new JSONObject();
            status.put("status", robotStatusTextView.getText().toString());
            jsonStatus.put(status);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mapInformation = new JSONObject();
        try {
            mapInformation.put("map", jsonMap);
            mapInformation.put("robot", jsonRobot);
            if (setWaypointStatus) {
                mapInformation.put("waypoint", jsonWaypoint);
                setWaypointStatus = false;
            }
            mapInformation.put("obstacle", jsonObstacle);
            mapInformation.put("arrow", jsonArrow);
            mapInformation.put("status", jsonStatus);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        showLog("Exiting getCreateJsonObject");
        return mapInformation;
    }

    public void resetMap() {
        showLog("Entering resetMap");
        TextView robotStatusTextView =  ((Activity)this.getContext()).findViewById(R.id.robotStatusTextView);
        ToggleButton manualAutoToggleBtn = ((Activity)this.getContext()).findViewById(R.id.manualAutoToggleBtn);
        Switch phoneTiltSwitch = ((Activity)this.getContext()).findViewById(R.id.phoneTiltSwitch);
        updateRobotAxis(0, 0, "None");
        robotStatusTextView.setText("status");
        sharedPreferences();
        editor.putString("receivedText", "");
        editor.putString("sentText", "");
        editor.commit();

        if (manualAutoToggleBtn.isChecked())
            manualAutoToggleBtn.toggle();
        this.toggleCheckedBtn("None");

        if (phoneTiltSwitch.isChecked()) {
            phoneTiltSwitch.toggle();
            phoneTiltSwitch.setText("TILT OFF");
        }

        receivedJsonObject = null;
        backupMapInformation = null;
        startCoord = new int[]{-1, -1};
        curCoord = new int[]{-1, -1};
        oldCoord = new int[]{-1, -1};
        robotDirection = "None";
        autoUpdate = false;
        arrowCoord = new ArrayList<>();
        obstacleCoord = new ArrayList<>();
        waypointCoord = new int[]{-1, -1};
        mapDrawn = false;
        canDrawRobot = false;
        validPosition = false;
        Bitmap arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_error);

        showLog("Exiting resetMap");
        this.invalidate();
    }

    private void sharedPreferences() {
        sharedPreferences = this.getContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    private void showLog(String message) {
        Log.d(TAG, message);
    }
}