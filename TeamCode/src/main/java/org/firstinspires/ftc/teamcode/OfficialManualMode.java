package org.firstinspires.ftc.teamcode;


import android.view.ViewDebug;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;

import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import java.util.ArrayList;
import java.util.Arrays;


@TeleOp(name = "Official Manual Mode", group = "Match")
public class OfficialManualMode extends LinearOpMode {
    private DcMotor _fl, _fr, _rl, _rr;
    private Servo _grip, _platform, _elbow, _shoulder;
    private double _armPosition = 0, _leftTrigger = 0, _rightTrigger = 0;
    private boolean logMode = false;
    private ArrayList<String> logArray = new ArrayList<>();
    private boolean firstTime = true;

    private ElapsedTime recentActionTime = new ElapsedTime();
    public double perStepSizePlatform = 0.001;
    public double perStepSizeShoulder = 0.01;
    public double perStepSizeElbow = 0.01;
    public double perStepSizeGrip = 0.01;
    public boolean enablePad1Control = false;
    public int minTimeOfTwoOperations = 20; //milliseconds, 0.05 second
    public double ratioPad2WheelSpeed = 0.1; //pad2 can control wheel at the ratio speed of pad1, 0 means pad2 stick can't wheels, 1 means same as pad1

    public double shoulderDefaultPosition = 0.15;
    public double shoulderMaxPosition = 0.94;
    public double shoulderMinPosition = 0.144;
    public double elbowMaxPosition = 0.06;
    public double elbowMinPosition = 0.5;
    public double elbowDefaultPosition = 0.90;
    public double platformDefaultPosition = 0.654 ;
    public double gripMinPosition = 0.07;
    public double gripMaxPosition = 0.7;

    public double elbowBothMaxShoulderBeginPosition = 0.65; //elbow start position when both_max begin
    public int    elbowTimeBothMaxShoulderBegin = 1000; // elbow remain at same position when shoulder begin up
    public int    shoulderTimeFromMinToMax = 4000;
    public double wheelTurnSpeed = 4.0;
    // "wheel_forward @10 @0.5", wheel_back 10inch and speed is 0.5
    // wheel_left/wheel_right/wheel_back
    // platform and shoulder elbow remain still, position / direction not changed

    // "time_wheel_left @3500 @0.2" : go straight left for 3500 milliseconds at speed 0.2
    // "time_wheel_right @5000 @0.3"
    // "time_wheel_forward @2000 @0.3" "time_wheel_back @3000 @0.3"
    // platform / shoulder / elbow position not changed with "time_wheel_" action

    // "wheel_turn_left @14 @0.3" turn left about 90 degree at speed 0.3
    // "wheel_turn_left @28 @0.2" turn about 180 degree at speed 0.2
    // platform / shoulder / elbow direction will change at same time

    // "platform_left @10" : platform turn left 10 times the perStepSize
    // "platform_right @20
    // "shoulder_up @10" "shoulder_down @20"
    // shoulder up or down 10 times the perStepSize
    // "elbow_up @10" "elbow_down @20"
    //
    // "both_min" shoulder and elbow will go down to the lowest position, ready to grab cone
    // "both_max" shoulder and elbow will go up to the highest position, ready to put cone to the pole

    // "position_shoulder @0.4", shoulder setPosition directly to 0.4
    // "position_platform @0.8", platform setPosition directly to 0.8
    // "position_elbow @ 0.5", elbow setPosition directly to 0.5

    // "grip_max" "grip_min" "grip_open @10" "grip_close @10"
    //
    public ArrayList<String> presetActionsLeft = new ArrayList<String>(Arrays.asList(
            "elbow_up @40",
            "grip_max",
            "sleep @400",
            "wheel_forward @9 @0.2",
            "grip_min",
            "sleep @400",
            "shoulder_up @75",
            "sleep @400",
            "wheel_forward @17 @0.2",
            "sleep @400",
            "wheel_right @30 @0.2",
            "platform_right @17",
            "sleep @400",
            "elbow_up @43",
            "sleep @500",
            "elbow_up @2",
            "sleep @500",
            "grip_max",
            "elbow_down @60",
            "platform_left @20",
            "wheel_back @28 @0.2",
            "shoulder_down @100",
            "sleep @400",
            "elbow_down @50"
    ));

    public ArrayList<String> presetActionsPad1X = new ArrayList<String>(Arrays.asList(
            "wheel_left @1 @0.1"
    ));
    public ArrayList<String> presetActionsPad1Y = new ArrayList<String>(Arrays.asList(
            "wheel_forward @1 @01"
    ));

    public ArrayList<String> presetActionsPad1A = new ArrayList<String>(Arrays.asList(
            "wheel_back @1 @01"
    ));

    public ArrayList<String> presetActionsPad1B = new ArrayList<String>(Arrays.asList(
            "wheel_right @1 @01"
    ));

    public ArrayList<String> presetActionsPad2X = presetActionsLeft;

    public ArrayList<String> presetActionsPad2Y = new ArrayList<String>(Arrays.asList(
            "both_max"
    ));
    public ArrayList<String> presetActionsPad2A = new ArrayList<String>(Arrays.asList(
            "both_min"
    ));
    public ArrayList<String> presetActionsPad2B = new ArrayList<String>(Arrays.asList(
            "wheel_turn_left @28 @0.3"
    ));
    public ArrayList<String> presetActionsBothMax = new ArrayList<String>(Arrays.asList(
    ));

    private ElapsedTime moreTimeToStart = new ElapsedTime();
    private boolean stopPresetAction = false;

    @Override
    public void runOpMode() throws InterruptedException {
        // Declare our motors
        // Make sure your ID's match your configuration
        _fl = hardwareMap.dcMotor.get("frontLeft");
        _fr = hardwareMap.dcMotor.get("frontRight");
        _rl = hardwareMap.dcMotor.get("rearLeft");
        _rr = hardwareMap.dcMotor.get("rearRight");

        // Reverse the right side motors
        // Reverse left motors if you are using NeveRests
        _fr.setDirection(DcMotorSimple.Direction.REVERSE);
        _rr.setDirection(DcMotorSimple.Direction.REVERSE);

        // RUN_USING_ENCODER, RUN_WITHOUT_ENCODER, RUN_TO_POSITION, STOP_AND_RESET_ENCODER
        _fl.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        _fr.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        _rl.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        _rr.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        _fl.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        _fr.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        _rl.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        _rr.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        // Send telemetry message to indicate successful Encoder reset
        telemetry.addData("Starting at",  " %7d :%7d :%7d :%7d",
                _fl.getCurrentPosition(), _fr.getCurrentPosition(), _rl.getCurrentPosition(), _rr.getCurrentPosition());
        //telemetry.addData("wheel device name", _fl.getDeviceName());
        //telemetry.addData("wheel manufacturer", _fl.getManufacturer());
        //telemetry.update();

        _platform = hardwareMap.get(Servo.class, "platform");
        _grip = hardwareMap.get(Servo.class, "grip");
        _elbow = hardwareMap.get(Servo.class, "elbow");
        _shoulder = hardwareMap.get(Servo.class, "shoulder");

        //_shoulder.resetDeviceConfigurationForOpMode();
        //_platform.resetDeviceConfigurationForOpMode();
        //_elbow.resetDeviceConfigurationForOpMode();
        //_grip.resetDeviceConfigurationForOpMode();
        //_platform.scaleRange(0.0, 1.0);
        //_shoulder.scaleRange(0.0, 1.0);
        //_elbow.scaleRange(0.0, 1.0);
        //_grip.scaleRange(0.0, 1.0);

        telemetry.addData("_grip device name", _grip.getDeviceName());
        telemetry.addData("_grip manufacturer", _grip.getManufacturer());
        telemetry.update();


       // _elbow.scaleRange(0.2,0.8);
        waitForStart();

        if (isStopRequested()) return;

        while (opModeIsActive()) {
            if (moreTimeToStart.milliseconds() < 500) {
                telemetry.addData("Waiting millisecond: ", moreTimeToStart.milliseconds()  );
                telemetry.update();
                continue;
                // do nothing;
            }
            if (firstTime) {
                firstTime = false;
                resetToPresetPosition(0);
            }
            controlArm();
            controlWheels();
        }
    }

    private ElapsedTime resetServoPositionTimer = new ElapsedTime();
    private void resetToPresetPosition(int presetMode) {
        //telemetry.addData("Preset position", presetMode);
        if (presetMode == 0) {
            logAction("Initial");
            // center the control arms
            _shoulder.setPosition(shoulderDefaultPosition);
            _elbow.setPosition(elbowDefaultPosition);
            _platform.setPosition(platformDefaultPosition);
            //replayActions(presetActionsShoulderUp);
        }
    }

    private boolean pwmEnable = true;
    public void resetServoPosition() {
        if (resetServoPositionTimer.seconds() > 5) {
            //_platform.getController().resetDeviceConfigurationForOpMode();
            _platform.getController().setServoPosition(0, 0.5);
            //ServoController.PwmStatus pwmStatus = _platform.getController().getPwmStatus();
            if (pwmEnable) {
                _platform.getController().pwmDisable();
                pwmEnable = false;
            }
            else {
                _platform.getController().pwmEnable();
                pwmEnable = true;
                _platform.setPosition(0.5);
            }
            //_platform.resetDeviceConfigurationForOpMode();
            //_shoulder.getController().setServoPosition(1, shoulderDefaultPosition);
            //_elbow.getController().setServoPosition(2, elbowDefaultPosition);
            resetServoPositionTimer.reset();
        }
    };

    private void controlWheels() {
        if (gamepad1.a) {
            replayActions(presetActionsPad1A);
            return;
        }
        if (gamepad1.b) {
            replayActions(presetActionsPad1B);
            return;
        }
        if (gamepad1.x) {
            replayActions(presetActionsPad1X);
            return;
        }
        if (gamepad1.y) {
            replayActions(presetActionsPad1Y);
            return;
        }
        if (gamepad1.options) {
            setLogMode(!logMode);
            return;
        }
        if (gamepad1.left_stick_button && gamepad1.right_stick_button) {
            //resetServoPosition();
            return;
        }
        if (gamepad1.left_bumper) {
            playAction("grip_open", true);
            return;
        }
        if (gamepad1.right_bumper) {
            playAction("grip_close", true);
            return;
        }
        if (gamepad1.left_stick_button && gamepad1.right_stick_button) {
            //resetServoPosition();
            return;
        }

        // enable for both controller
        double y, x , rx, denominator;
        double frontLeftPower, backLeftPower, frontRightPower, backRightPower, speedmultiplier;
        y = gamepad1.left_stick_y * 0.5; // Remember, this is reversed!
        x = -gamepad1.left_stick_x * 1.1 * 0.5; // Counteract imperfect strafing
        rx = -gamepad1.right_stick_x * 0.5;

        //if (gamepad1.left_stick_x != 0 || gamepad1.right_stick_x != 0) {
        y = gamepad1.left_stick_y * 0.5; // Remember, this is reversed!
        x = -gamepad1.left_stick_x * 1.1 * 0.5; // Counteract imperfect strafing
        rx = -gamepad1.right_stick_x * 0.5;

        // Denominator is the largest motor power (absolute value) or 1
        // This ensures all the powers maintain the same ratio, but only when
        // at least one is out of the range [-1, 1]
        denominator = Math.max(Math.abs(y) + Math.abs(x) + Math.abs(rx), 1);
        frontLeftPower = (y + x + rx) / denominator;
        backLeftPower = (y - x + rx) / denominator;
        frontRightPower = (y - x - rx) / denominator;
        backRightPower = (y + x - rx) / denominator;
        speedmultiplier = 1;
        if (gamepad1.left_trigger > 0) {
            speedmultiplier = 2;
        } else if (gamepad1.right_trigger > 0) {
            speedmultiplier = 0.5 * wheelTurnSpeed;
        } else {
            speedmultiplier = 1;
        }
       //}
        /*
        else {
            // read data from pad2
            // ignore all of the left_stick signal (controller has some problem)
            y = gamepad2.left_stick_y * 0.5; // Remember, this is reversed!
            x = -gamepad2.left_stick_x * 1.1 * 0.5; // Counteract imperfect strafing
            rx = -gamepad2.right_stick_x * 0.5;

            // Denominator is the largest motor power (absolute value) or 1
            // This ensures all the powers maintain the same ratio, but only when
            // at least one is out of the range [-1, 1]
            denominator = Math.max(Math.abs(y) + Math.abs(x) + Math.abs(rx), 1);
            frontLeftPower = (y + x + rx) / denominator;
            backLeftPower = (y - x + rx) / denominator;
            frontRightPower = (y - x - rx) / denominator;
            backRightPower = (y + x - rx) / denominator;
            speedmultiplier = 1;
            if (gamepad2.left_trigger > 0) {
                speedmultiplier = 2;
            } else if (gamepad2.right_trigger > 0) {
                speedmultiplier = 0.5;
            } else {
                speedmultiplier = 1;
            }
            speedmultiplier *= ratioPad2WheelSpeed;
        }
        */

        _fl.setPower(frontLeftPower * speedmultiplier);
        _rl.setPower(backLeftPower * speedmultiplier);
        _fr.setPower(frontRightPower * speedmultiplier);
        _rr.setPower(backRightPower * speedmultiplier);

        //telemetry.addData("FLSpd", frontLeftPower * speedmultiplier);
        //telemetry.addData("RRSpd", backRightPower * speedmultiplier);
        //telemetry.update();

    }

    void setElbowPositionAlongWithShoulder(double shoulderPosition) {
        double ratio = (elbowMaxPosition - elbowMinPosition) / (shoulderMaxPosition - shoulderMinPosition);
        double elbowPosition = ratio * (shoulderPosition - shoulderMinPosition) + elbowMinPosition;
        _elbow.setPosition(elbowPosition);
    }


    private void controlArm() {
        if (gamepad2.x) {
            if (gamepad2.left_stick_button) {
               // replayActions(presetActionsPad2X);
            }
            return;
        }
        if (gamepad2.a) {
            if (gamepad2.left_stick_button) {
                replayActions(presetActionsPad2A);
            }
            return;
        }
        if (gamepad2.b) {
            if (gamepad2.left_stick_button) {
               replayActions(presetActionsPad2B);
            }
            return;
        }
        if (gamepad2.y) {
            if (gamepad2.left_stick_button) {
               replayActions(presetActionsPad2Y);
            }
            return;
        }

        if (gamepad2.left_bumper || (enablePad1Control && gamepad1.left_bumper)) {
            playAction("grip_max", true);
            return;
        }
        if (gamepad2.right_bumper || (enablePad1Control && gamepad1.right_bumper)) {
            playAction("grip_min", true);
            return;
        }
        if (gamepad2.dpad_up || (enablePad1Control && gamepad1.dpad_up)) {
            playAction("shoulder_up", true);
            return;
        }
        if (gamepad2.dpad_down || (enablePad1Control && gamepad1.dpad_down)) {
            playAction("shoulder_down", true);
            return;
        }
        if (gamepad2.dpad_left || (enablePad1Control && gamepad1.dpad_left)) {
            playAction("platform_left", true);
            return;
        }
        if (gamepad2.dpad_right || (enablePad1Control && gamepad1.dpad_right)) {
            playAction("platform_right", true);
            return;
        }
        if ((gamepad2.left_trigger > 0 && gamepad2.right_trigger == 0) ||
                (enablePad1Control && gamepad1.left_trigger > 0 && gamepad1.right_trigger == 0)) {
            playAction("elbow_up", true);
            return;
        }
        if ((gamepad2.right_trigger > 0 && gamepad2.left_trigger == 0) ||
                (enablePad1Control && gamepad1.right_trigger > 0 && gamepad1.left_trigger == 0)) {
            playAction("elbow_down", true);
            return;
        }
        if (gamepad2.left_stick_button) {
            //playAction("left_stick_button", true);
            return;
        }
        if (gamepad2.right_stick_button) {
            //playAction("right_stick_button", true);
            return;
        }

    }

    private boolean shoulderMoved = false;
    private void playAction(String actionName, boolean ignoreRecent) {
        if (ignoreRecent) {
            if (recentActionTime.milliseconds() < minTimeOfTwoOperations) {
                // too close to last action, ignore it
                return;
            }
            else {
                recentActionTime.reset();
            }
        }
        logAction(actionName);
        if (actionName.equals("platform_left")) {
            if (_platform.getPosition() <= (1 - perStepSizePlatform))
                _platform.setPosition(_platform.getPosition() + perStepSizePlatform);
        }
        else if (actionName.equals("platform_right")) {
            if (_platform.getPosition() >= perStepSizePlatform)
                _platform.setPosition(_platform.getPosition() - perStepSizePlatform);
        }
        else if (actionName.equals("shoulder_up")) {
            double currentShoulderPosition = _shoulder.getPosition();
            if (currentShoulderPosition < shoulderMaxPosition) {
                _shoulder.setPosition(currentShoulderPosition + perStepSizeShoulder);
                setElbowPositionAlongWithShoulder(currentShoulderPosition + perStepSizeShoulder);
                //_elbow.setPosition(-0.66*_shoulder.getPosition() + 0.62);
            }
            /*
            if (shoulderMoved) {
                shoulderMoved = false;
                if (_shoulder.getDirection() == Servo.Direction.FORWARD)
                    _shoulder.setDirection(Servo.Direction.REVERSE);
                else
                    _shoulder.setDirection(Servo.Direction.FORWARD);
                _shoulder.setPosition(0);
            }
            else {
                if (_shoulder.getPosition() >= perStepSize) {
                    _shoulder.setPosition(_shoulder.getPosition() - perStepSize);
                    shoulderMoved = true;
                }
            }
            */
        }
        else if (actionName.equals("shoulder_down")) {
            double currentShoulderPosition = _shoulder.getPosition();
            if (_shoulder.getPosition() > shoulderMinPosition) {
                _shoulder.setPosition(currentShoulderPosition - perStepSizeShoulder);
                setElbowPositionAlongWithShoulder(currentShoulderPosition - perStepSizeShoulder);
            }
            /*
            if (shoulderMoved) {
                if (_shoulder.getDirection() == Servo.Direction.FORWARD)
                    _shoulder.setDirection(Servo.Direction.REVERSE);
                else
                    _shoulder.setDirection(Servo.Direction.FORWARD);
                shoulderMoved = false;
                _shoulder.setPosition(_shoulder.getPosition());
            }
            else {
                if (_shoulder.getPosition() <= (1 - perStepSize)) {
                    _shoulder.setPosition(_shoulder.getPosition() + perStepSize);
                    shoulderMoved = true;
                }
            }
            */
        }
        else if (actionName.equals("grip_open")) {
            if (_grip.getPosition() < gripMaxPosition)
                _grip.setPosition(_grip.getPosition() + perStepSizeGrip);
        }
        else if (actionName.equals("grip_max")) {
            _grip.setPosition(gripMaxPosition);
        }
        else if (actionName.equals("grip_close")) {
            if (_grip.getPosition() > gripMinPosition)
                _grip.setPosition(_grip.getPosition() - perStepSizeGrip);
        }
        else if (actionName.equals("grip_min")) {
            _grip.setPosition(gripMinPosition);
        }
        else if (actionName.equals("elbow_up")) {
            if (_elbow.getPosition() > perStepSizeElbow) {
                _elbow.setPosition(_elbow.getPosition() - perStepSizeElbow);
            }
        }
        else if (actionName.equals("elbow_down")) {
            if (_elbow.getPosition() < (1 - perStepSizeElbow)) {
                _elbow.setPosition(_elbow.getPosition() + perStepSizeElbow);
            }

        }

    }


    private void setLogMode(boolean mode) {
        if (logMode == mode)
            return;
        if (mode == true) {
            logMode = true;
            logArray.clear();
            //System.out.println("Start log...");
            telemetry.addData("Start log...", logArray.size());
            telemetry.update();
        }
        else if (mode == false) {
            telemetry.addData("Stop log...", logArray.size());
            logMode = false;
            // print out the moves / buttons pressed since log start;
            //System.out.println("Operations: " + logArray);
            for (int index = 0; index < logArray.size(); index++) {
                String s = Integer.toString(index);
                s += " ";
                s += logArray.get(index);
                telemetry.addData("Operations: ", s);
                telemetry.log().add(s);
            }
            //telemetry.addData("Operations: ", logArray);
            telemetry.update();

        }
    }

    private void logAction(String s) {
        if (logMode == true) {
            logArray.add(s);
        }
        telemetry.addData("logAction", s);

        telemetry.addData("_platform ", _platform.getPosition());
        telemetry.addData("_platform servo ", _platform.getController().getServoPosition(0));
        telemetry.addData("_shoulder ", _shoulder.getPosition());
        telemetry.addData("_shoulder servo ", _shoulder.getController().getServoPosition(1));
        telemetry.addData("_elbow ", _elbow.getPosition());
        telemetry.addData("_elbow servo ", _elbow.getController().getServoPosition(2));
        telemetry.addData("_grip ", _grip.getPosition());
        telemetry.addData("_grip servo ", _grip.getController().getServoPosition(3));
        telemetry.update();
    }


    private void replayActions(ArrayList<String> list) {
        for (int i = 0; i < list.size(); i++) {
            if (gamepad2.right_stick_button) {
                stopPresetAction = false;
                return;
            }
            String s = Integer.toString(i);
            String actionName = list.get(i);
            s += actionName;
            telemetry.addData("replay: ", s);

            //
            //"platform_left"
            //"platform_left@10"
            //"platform_left @ 10 @ 0.05"
            //
            String[] splitStrings = actionName.split("@", 3);
            for (int k = 0; k < splitStrings.length; k++) {
                splitStrings[k] = splitStrings[k].trim();
            }
            if (splitStrings.length == 0) {
                return;
            }
            boolean isWheelAction = splitStrings[0].startsWith("wheel");
            int repeatTimes = 1;

            if (splitStrings[0].contains("sleep")) {
                repeatTimes = Integer.parseInt(splitStrings[1]);
                sleep(repeatTimes);
            }
            else if (splitStrings[0].startsWith("both")) {
                shoulderElbowBoth(splitStrings);
            }
            else if (splitStrings[0].startsWith("time_wheel")) {
                timeWheel(splitStrings[0], splitStrings[1], splitStrings[2]);
            }
            else if (splitStrings[0].startsWith("position") && splitStrings.length >= 2) {
                position(splitStrings[0], splitStrings[1]);
            }
            else if (isWheelAction && splitStrings.length >= 3) {
                wheel(splitStrings[0], splitStrings[1], splitStrings[2], 60);
            }
            else {
                if (splitStrings.length >= 2)
                    repeatTimes = Integer.parseInt(splitStrings[1]);
                for (int j = 0; j < repeatTimes; j++) {
                    if (gamepad2.right_stick_button)
                    {
                        stopPresetAction = false;
                        return;
                    }
                    playAction(splitStrings[0], false);
                    sleep(10);
                }
            }
        }
        telemetry.update();

    }

    public void shoulderElbowBoth(String[] splitStrings) {
        if (splitStrings[0].equals("both_min")) {
            double shoulderPosition = _shoulder.getPosition();
            _shoulder.setPosition(shoulderMinPosition);
            if (shoulderPosition < shoulderMaxPosition * 2 / 3) {
                _elbow.setPosition(elbowMinPosition);
            }
            else {
                int remainTime = shoulderTimeFromMinToMax - 1000;
                int perStepSleepTime = 100;
                double elbowPosition = _elbow.getPosition();
                double stepSize = (elbowMinPosition - elbowPosition) / (remainTime / perStepSleepTime);
                while (remainTime > 0) {
                    telemetry.addData("stepSize", stepSize);
                    telemetry.addData("remainTime", remainTime);
                    logAction("both_min");
                    elbowPosition += stepSize;
                    if (elbowPosition < elbowMinPosition)
                        _elbow.setPosition(elbowPosition);
                    sleep(perStepSleepTime);
                    remainTime -= perStepSleepTime;
                }
            }
            //_grip.setPosition(gripMaxPosition);
        }
        else if (splitStrings[0].equals("both_max")) {
            double shoulderPosition = _shoulder.getPosition();
            _shoulder.setPosition(shoulderMaxPosition);
            if (shoulderPosition > shoulderMaxPosition * 2 / 3) {
                _elbow.setPosition(elbowMaxPosition);
            }
            else {
                _elbow.setPosition(elbowBothMaxShoulderBeginPosition);
                sleep(elbowTimeBothMaxShoulderBegin);
                int remainTime = shoulderTimeFromMinToMax - elbowTimeBothMaxShoulderBegin;
                int perStepSleepTime = 100;
                double stepSize = (elbowBothMaxShoulderBeginPosition - elbowMaxPosition) / (remainTime / perStepSleepTime);
                double elbowPosition = elbowBothMaxShoulderBeginPosition;
                while (remainTime > 0) {
                    telemetry.addData("stepSize", stepSize);
                    telemetry.addData("remainTime", remainTime);
                    logAction("both_max");
                    elbowPosition -= stepSize;
                    if (elbowPosition >= elbowMaxPosition)
                        _elbow.setPosition(elbowPosition);
                    sleep(perStepSleepTime);
                    remainTime -= perStepSleepTime;
                }
            }
            //logAction("both_max_end");
            //_elbow.setPosition(0.5);
            //sleep(2000);
        }
    }

    public void position(String target, String sPosition) {
        double targetPosition = Double.parseDouble(sPosition);
        if (targetPosition < 0 || targetPosition > 1)
            return;
        if (target.equals("position_platform")) {
            _platform.setPosition(targetPosition);
        }
        else if (target.equals("position_shoulder")) {
            _shoulder.setPosition(targetPosition);
        }
        else if (target.equals("position_elbow")) {
            _elbow.setPosition(targetPosition);
        }
    }

    public void timeWheel(String direction, String sMilliSeconds, String sSpeed) {
        int timeMilliSeconds = Integer.parseInt(sMilliSeconds);
        double speed = Double.parseDouble(sSpeed);
        wheelRunTime.reset();

        //default: left
        double frontLeftPower = -speed;
        double frontRightPower = speed;
        double rearLeftPower = speed;
        double rearRightPower = -speed;

        if (direction.contains("time_wheel_right")) {
            frontLeftPower = -frontLeftPower;
            frontRightPower = -frontRightPower;
            rearLeftPower = -rearLeftPower;
            rearRightPower = -rearRightPower;
        }
        else if (direction.contains("time_wheel_forward")) {
            frontLeftPower = speed;
            frontRightPower = speed;
            rearLeftPower = speed;
            rearRightPower = speed;
        }
        else if (direction.contains("time_wheel_back")) {
            frontLeftPower = -speed;
            frontRightPower = -speed;
            rearLeftPower = -speed;
            rearRightPower = -speed;
        }

        _fl.setPower(frontLeftPower);
        _fr.setPower(frontRightPower);
        _rl.setPower(rearLeftPower);
        _rr.setPower(rearRightPower);

        while (wheelRunTime.milliseconds() < timeMilliSeconds) {
            telemetry.addData("Direction", direction);
            telemetry.addData("speed", speed);
            telemetry.addData("run for ms:", timeMilliSeconds);
            telemetry.addData("time", wheelRunTime.milliseconds());
            telemetry.update();
        }

        // Stop all motion;
        _fl.setPower(0);
        _fr.setPower(0);
        _rl.setPower(0);
        _rr.setPower(0);
    }

    // Calculate the COUNTS_PER_INCH for your specific drive train.
    // Go to your motor vendor website to determine your motor's COUNTS_PER_MOTOR_REV
    // For external drive gearing, set DRIVE_GEAR_REDUCTION as needed.
    // For example, use a value of 2.0 for a 12-tooth spur gear driving a 24-tooth spur gear.
    // This is gearing DOWN for less speed and more torque.
    // For gearing UP, use a gear ratio less than 1.0. Note this will affect the direction of wheel rotation.
    //static final double     COUNTS_PER_MOTOR_REV    = 1440 ;    // eg: TETRIX Motor Encoder
    static final double     COUNTS_PER_MOTOR_REV    = 512 ;
    static final double     DRIVE_GEAR_REDUCTION    = 1.0 ;     // No External Gearing.
    static final double     WHEEL_DIAMETER_INCHES   = 3.77953 ;     // For figuring circumference
    static final double     COUNTS_PER_INCH         = (COUNTS_PER_MOTOR_REV * DRIVE_GEAR_REDUCTION) /
            (WHEEL_DIAMETER_INCHES * 3.14159265);
    private ElapsedTime     wheelRunTime = new ElapsedTime();

    /*
     *  Method to perform a relative move, based on encoder counts.
     *  Encoders are not reset as the move is based on the current position.
     *  Move will stop if any of three conditions occur:
     *  1) Move gets to the desired position
     *  2) Move runs out of time
     *  3) Driver stops the opmode running.
     */

    public void wheel(String direction, String sDistanceInches, String sSpeed, double timeoutS) {
        double distanceInches = Double.parseDouble(sDistanceInches);
        double speed = Double.parseDouble(sSpeed);
        int newFrontLeftTarget = 0;
        int newFrontRightTarget = 0;
        int newRearLeftTarget = 0;
        int newRearRightTarget = 0;

        // reset the timeout time and start motion.
        wheelRunTime.reset();
        double frontLeftPower = speed;
        double frontRightPower = speed;
        double rearLeftPower = speed;
        double rearRightPower = speed;

        if (direction.contains("forward") || direction.equals("wheel_forward") ) {
            // Determine new target position, and pass to motor controller
            newFrontLeftTarget = _fl.getCurrentPosition() - (int)(distanceInches * COUNTS_PER_INCH);
            newFrontRightTarget = _fr.getCurrentPosition() - (int)(distanceInches * COUNTS_PER_INCH);
            newRearLeftTarget = _rl.getCurrentPosition() - (int)(distanceInches * COUNTS_PER_INCH);
            newRearRightTarget = _rr.getCurrentPosition() - (int)(distanceInches * COUNTS_PER_INCH);

            frontLeftPower = speed;
            frontRightPower = 0 - speed;
            rearLeftPower = speed;
            rearRightPower = 0 - speed;
        }
        else if (direction.contains("back") || direction.equals("wheel_back") ) {
            // Determine new target position, and pass to motor controller
            newFrontLeftTarget = _fl.getCurrentPosition() + (int)(distanceInches * COUNTS_PER_INCH);
            newFrontRightTarget = _fr.getCurrentPosition() + (int)(distanceInches * COUNTS_PER_INCH);
            newRearLeftTarget = _rl.getCurrentPosition() + (int)(distanceInches * COUNTS_PER_INCH);
            newRearRightTarget = _rr.getCurrentPosition() + (int)(distanceInches * COUNTS_PER_INCH);

            frontLeftPower = speed;
            frontRightPower = speed;
            rearLeftPower = speed;
            rearRightPower = speed;
        }
        else if (direction.equals("wheel_turn_left") ) {
            newFrontLeftTarget = _fl.getCurrentPosition() + (int)(distanceInches * COUNTS_PER_INCH);
            newFrontRightTarget = _fr.getCurrentPosition() - (int)(distanceInches * COUNTS_PER_INCH);
            newRearLeftTarget = _rl.getCurrentPosition() + (int)(distanceInches * COUNTS_PER_INCH);
            newRearRightTarget = _rr.getCurrentPosition() - (int)(distanceInches * COUNTS_PER_INCH);

            frontLeftPower = 0 - speed;
            frontRightPower = speed;
            rearLeftPower = 0 - speed;
            rearRightPower = speed;
        }
        else if (direction.equals("wheel_turn_right") ) {
            newFrontLeftTarget = _fl.getCurrentPosition() - (int)(distanceInches * COUNTS_PER_INCH);
            newFrontRightTarget = _fr.getCurrentPosition() + (int)(distanceInches * COUNTS_PER_INCH);
            newRearLeftTarget = _rl.getCurrentPosition() - (int)(distanceInches * COUNTS_PER_INCH);
            newRearRightTarget = _rr.getCurrentPosition() + (int)(distanceInches * COUNTS_PER_INCH);

            frontLeftPower = speed;
            frontRightPower = 0 - speed;
            rearLeftPower = speed;
            rearRightPower = 0 - speed;
        }
        else if (direction.equals("wheel_left") ) {
            newFrontLeftTarget = _fl.getCurrentPosition() + (int)(distanceInches * COUNTS_PER_INCH);
            newFrontRightTarget = _fr.getCurrentPosition() - (int)(distanceInches * COUNTS_PER_INCH);
            newRearLeftTarget = _rl.getCurrentPosition() - (int)(distanceInches * COUNTS_PER_INCH);
            newRearRightTarget = _rr.getCurrentPosition() + (int)(distanceInches * COUNTS_PER_INCH);

            frontLeftPower = speed;
            frontRightPower = 0 - speed;
            rearLeftPower = 0 - speed;
            rearRightPower = speed;
        }
        else if (direction.equals("wheel_right") ) {
            newFrontLeftTarget = _fl.getCurrentPosition() - (int)(distanceInches * COUNTS_PER_INCH);
            newFrontRightTarget = _fr.getCurrentPosition() + (int)(distanceInches * COUNTS_PER_INCH);
            newRearLeftTarget = _rl.getCurrentPosition() + (int)(distanceInches * COUNTS_PER_INCH);
            newRearRightTarget = _rr.getCurrentPosition() - (int)(distanceInches * COUNTS_PER_INCH);

            frontLeftPower = 0 - speed;
            frontRightPower = speed;
            rearLeftPower = speed;
            rearRightPower = 0 - speed;
        }

        _fl.setTargetPosition(newFrontLeftTarget);
        _fr.setTargetPosition(newFrontRightTarget);
        _rl.setTargetPosition(newRearLeftTarget);
        _rr.setTargetPosition(newRearRightTarget);

        // Turn On RUN_TO_POSITION
        _fl.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        _fr.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        _rl.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        _rr.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        _fl.setPower(frontLeftPower);
        _fr.setPower(frontRightPower);
        _rl.setPower(rearLeftPower);
        _rr.setPower(rearRightPower);

        // keep looping while we are still active, and there is time left, and both motors are running.
        // Note: We use (isBusy() && isBusy()) in the loop test, which means that when EITHER motor hits
        // its target position, the motion will stop.  This is "safer" in the event that the robot will
        // always end the motion as soon as possible.
        // However, if you require that BOTH motors have finished their moves before the robot continues
        // onto the next step, use (isBusy() || isBusy()) in the loop test.
        while (opModeIsActive() &&
                (wheelRunTime.seconds() < timeoutS) &&
                (_fl.isBusy() && _fr.isBusy() && _rl.isBusy() && _rr.isBusy())) {

            // Display it for the driver.
            telemetry.addData("direction: ", direction);
            telemetry.addData("DistanceInches: ", distanceInches);
            telemetry.addData("Running to",  " %7d :%7d :%7d :%7d", newFrontLeftTarget,  newFrontRightTarget, newRearLeftTarget, newRearRightTarget);
            telemetry.addData("Currently at",  " at %7d :%7d :%7d :%7d", _fl.getCurrentPosition(), _fr.getCurrentPosition(), _rl.getCurrentPosition(), _rr.getCurrentPosition());
            telemetry.update();
        }

        // Stop all motion;
        _fl.setPower(0);
        _fr.setPower(0);
        _rl.setPower(0);
        _rr.setPower(0);

        // Turn off RUN_TO_POSITION
        // RUN_USING_ENCODER, RUN_WITHOUT_ENCODER, RUN_TO_POSITION, STOP_AND_RESET_ENCODER
        _fl.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        _fr.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        _rl.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        _rr.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        //sleep(100);   // optional pause after each move.
    }
 }


