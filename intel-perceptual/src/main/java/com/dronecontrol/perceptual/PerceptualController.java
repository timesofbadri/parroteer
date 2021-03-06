package com.dronecontrol.perceptual;

import com.google.inject.Inject;
import com.dronecontrol.perceptual.components.DetectionComponent;
import com.dronecontrol.perceptual.components.GestureComponent;
import com.dronecontrol.perceptual.components.PictureComponent;
import com.dronecontrol.perceptual.data.DetectionType;
import com.dronecontrol.perceptual.data.body.BodyPart;
import com.dronecontrol.perceptual.injection.Context;
import com.dronecontrol.perceptual.listeners.DetectionListener;
import com.dronecontrol.perceptual.listeners.GestureListener;
import com.dronecontrol.perceptual.listeners.PictureListener;
import intel.pcsdk.PXCUPipelineJNI;
import org.apache.log4j.Logger;

public class PerceptualController {
    private final Logger logger = Logger.getLogger(PerceptualController.class);

    private final PerceptualPipeline pipeline;

    private final CamProcessor camProcessor;

    private final PictureComponent pictureComponent;
    private final DetectionComponent detectionComponent;
    private final GestureComponent gestureComponent;

    public static PerceptualController buildPerceptualController() {
        return Context.getBean(PerceptualController.class);
    }

    @Inject
    public PerceptualController(PerceptualPipeline pipeline, CamProcessor listener,
                                PictureComponent pictureComponent, DetectionComponent detectionComponent, GestureComponent gestureComponent) {
        this.pipeline = pipeline;
        this.camProcessor = listener;
        this.pictureComponent = pictureComponent;
        this.detectionComponent = detectionComponent;
        this.gestureComponent = gestureComponent;
    }

    public void connect() {
        logger.info("Connecting to intel perceptual programming creative controller");
        pipeline.Init(PXCUPipelineJNI.GESTURE | PXCUPipelineJNI.COLOR_VGA);
        logger.info("Connection successfully established!");

        startCamProcessorThread();
    }

    public void startCamProcessorThread() {
        new Thread(camProcessor).start();
    }

    public void disconnect() {
        camProcessor.stop();
    }

    public void addPictureListener(PictureListener listener) {
        pictureComponent.addPictureListener(listener);
    }

    public void removePictureListener(PictureListener listener) {
        pictureComponent.removePictureListener(listener);
    }

    public void addGestureListener(GestureListener listener) {
        gestureComponent.addGestureListener(listener);
    }

    public void removeGestureListener(GestureListener listener) {
        gestureComponent.removeGestureListener(listener);
    }

    public <T extends BodyPart> void addDetectionListener(DetectionType<T> detectionType, DetectionListener<T> listener) {
        detectionComponent.addDetectionListener(detectionType, listener);
    }

    public <T extends BodyPart> void removeDetectionListener(DetectionType<T> detectionType, DetectionListener<T> listener) {
        detectionComponent.removeDetectionListener(detectionType, listener);
    }
}