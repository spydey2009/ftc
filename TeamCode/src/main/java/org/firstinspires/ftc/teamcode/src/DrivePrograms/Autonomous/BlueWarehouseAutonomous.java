package org.firstinspires.ftc.teamcode.src.DrivePrograms.Autonomous;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.teamcode.src.Utills.AutoObjDetectionTemplate;
import org.firstinspires.ftc.teamcode.src.robotAttachments.Sensors.RobotVoltageSensor;
import org.firstinspires.ftc.teamcode.src.robotAttachments.Subsystems.LinearSlide;


@Autonomous(name = "BlueWarehouseAutonomous")
public class BlueWarehouseAutonomous extends AutoObjDetectionTemplate {
    LinearSlide slide;

    @Override
    public void runOpMode() throws InterruptedException {
        this.initAll();
        MarkerPosition Pos = MarkerPosition.NotSeen;
        RobotVoltageSensor s = new RobotVoltageSensor(hardwareMap);
        slide = new LinearSlide(hardwareMap, "slide_motor", s, this::opModeIsActive, this::isStopRequested);
        Thread t = new Thread(slide);
        slide.setTargetLevel(LinearSlide.HeightLevels.Down);
        this.initVuforia();
        this.initTfod();
        this.activate();

        while (!isStarted()) {
            Pos = this.getAverageOfMarker(10, 100);
            telemetry.addData("Position", Pos);
            telemetry.update();
        }


        odometry.setPosition(133, 63, 270);

        waitForStart();
        tfod.shutdown();
        vuforia.close();
        System.gc();
        t.start();


        switch (Pos) {
            case NotSeen:
                telemetry.addData("position", " is far left");
                telemetry.update();

                driveSystem.moveToPosition(120, 84, 1);

                slide.setTargetLevel(LinearSlide.HeightLevels.TopLevel);
                intake.setServoDown();

                Thread.sleep(500);
                driveSystem.moveToPosition(111, 84, 1);
                Thread.sleep(500);
                intake.intakeOn();
                Thread.sleep(1000);
                intake.intakeOff();
                driveSystem.moveToPosition(120, 84, 1);
                intake.setServoUp();
                driveSystem.turnTo(190, .5);
                //Thread.sleep(500);
                driveSystem.moveToPosition(131, 63, 2);

                driveSystem.moveToPosition(132, 24, 1);


                break;
            case Right:
                telemetry.addData("position", " is right");
                telemetry.update();
                driveSystem.moveToPosition(120, 84, 1);

                slide.setTargetLevel(LinearSlide.HeightLevels.BottomLevel);


                break;
            case Left:
                telemetry.addData("position", "is center");
                telemetry.update();
                driveSystem.moveToPosition(120, 84, 1);

                slide.setTargetLevel(LinearSlide.HeightLevels.MiddleLevel);


                break;


        }
        telemetry.update();
        while (opModeIsActive() && !isStopRequested()) ;

    }
}


