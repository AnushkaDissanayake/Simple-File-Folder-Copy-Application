package controller;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXTextArea;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import java.io.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MainFormController {
    public JFXButton btnSelectFiles;
    public JFXButton btnDir;
    public JFXButton btnCopy;
    public Rectangle recProgressBar;

    public Label lblFolder;
    public JFXRadioButton rdoFiles;
    public JFXRadioButton rdoFolders;
    public JFXTextArea txtSelectedFiles;
    public Label lblProgress;
    public Rectangle recContainer;
    public Label lblSize;
    public Label lblNumberOfFiles;

    private List<File> srcFiles;
    private File srcFolder;

    private File destDir;
    double fileSize=0;
    int totalNumberOfCopiedFiles=0;
    int totalFiles=0;
    int totalRead=0;
    int k;
    private ArrayList<File> returnFileList = new ArrayList<>();
    private ArrayList<File> returnDirectoryList = new ArrayList<>();
    //private  String

    public void initialize() {
        btnCopy.setDisable(true);
    }


    public void btnSelectFilesOnAction(ActionEvent actionEvent) {

        if(rdoFiles.isSelected()){
            FileChooser fileChooser = new FileChooser();
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            fileChooser.setTitle("Select files to copy");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files (*.*)", "*.*"));

            srcFiles = fileChooser.showOpenMultipleDialog(txtSelectedFiles.getScene().getWindow());
            totalFiles=srcFiles.size();
            if (srcFiles != null) {
                String fileNameLabel="";
                for (File file:srcFiles){
                    fileNameLabel+=(""+file.getName()+ "  ,  " + (file.length() / 1024.0) + "KB \n" );
                    fileSize+=file.length();
                }
                txtSelectedFiles.setText(fileNameLabel);
            } else {
                txtSelectedFiles.setText("No Selected Files");
            }

        }else {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            directoryChooser.setTitle("Select folder to copy");
            srcFolder = directoryChooser.showDialog(txtSelectedFiles.getScene().getWindow());
            if(srcFolder !=null) {
                getFiles(srcFolder);
                totalFiles = returnFileList.size();
                if (returnFileList != null) {

                    for (File file : returnFileList) {

                        fileSize += file.length();
                    }

                    txtSelectedFiles.setText("" + srcFolder.getAbsolutePath() + "  size = " + formatNumber(fileSize/1024) + " KB");
                } else {
                    txtSelectedFiles.setText("No Selected Files");
                }

            }

        }
        /* Active and Disable Copy button */
        boolean bool;
        if(rdoFiles.isSelected()){
            bool = srcFiles.isEmpty();
        }else{
            bool= returnFileList.isEmpty() && returnDirectoryList.isEmpty();
        }

        btnCopy.setDisable(destDir == null|| bool);

    }

    public void btnDirOnAction(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select a destination folder");
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        destDir = directoryChooser.showDialog(lblFolder.getScene().getWindow());

        if (destDir != null) {
            lblFolder.setText(destDir.getAbsolutePath());
        } else {
            lblFolder.setText("No folder selected");
        }

        /* Active and Disable Copy button */
        boolean bool;
        if(rdoFiles.isSelected()){
            bool = srcFiles.isEmpty();
        }else{
            bool= returnFileList.isEmpty() && returnDirectoryList.isEmpty();
        }

        btnCopy.setDisable(destDir == null|| bool);
    }


    public void btnCopyOnAction(ActionEvent actionEvent) throws IOException, InterruptedException {
        rdoFiles.setDisable(true);
        rdoFolders.setDisable(true);

        if (rdoFiles.isSelected()){
            totalFiles=srcFiles.size();

            for(File srfile:srcFiles) {
                File destinationFile = new File(destDir, srfile.getName());
                if (!destinationFile.exists()) {
                    destinationFile.createNewFile();
                } else {

                    Optional<ButtonType> result = new Alert(Alert.AlertType.INFORMATION, "" + srfile.getName() + " File already exists. Do you want to overwrite?", ButtonType.YES, ButtonType.NO).showAndWait();
                    if (result.get() == ButtonType.NO) {
                        return;
                    }


                }

                var task = new Task<>() {
                    @Override
                    protected Object call() throws Exception {
                        FileInputStream fis = new FileInputStream(srfile);
                        FileOutputStream fos = new FileOutputStream(destinationFile);
                        BufferedInputStream bis = new BufferedInputStream(fis);
                        BufferedOutputStream bos = new BufferedOutputStream(fos);

                        while(true) {
                            byte[] buffer = new byte[1024 * 50];
                            int read = bis.read(buffer);

                            if (read==-1){
                                break;
                            }
                            bos.write(buffer, 0, read);
                            totalRead+=read;
                            updateProgress(totalRead,fileSize);
                        }


                        updateProgress(fileSize,fileSize);
                        bos.close();
                        bis.close();
                        return null;
                    }
                };
                task.progressProperty().addListener(new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {

                        lblNumberOfFiles.setText("Files : "+totalNumberOfCopiedFiles+" / "+totalFiles);
                        recProgressBar.setWidth((recContainer.getWidth() / fileSize) * totalRead);
                        lblProgress.setText("Progress: " + formatNumber2(totalRead * 1.0 / fileSize * 100) + "%");
                        lblSize.setText(formatNumber(totalRead / 1024.0) + " / " + formatNumber(fileSize / 1024.0) + " Kb");

                    }
                });
                task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                    @Override
                    public void handle(WorkerStateEvent workerStateEvent) {
                        totalNumberOfCopiedFiles+=1;
                        if (totalNumberOfCopiedFiles==totalFiles){
                            recProgressBar.setWidth(recContainer.getWidth());
                            lblNumberOfFiles.setText("Files : "+totalFiles+" / "+totalFiles);
                            new Alert(Alert.AlertType.INFORMATION, "File has been copied successfully").showAndWait();
                            lblNumberOfFiles.setText("Files : 0 / 0");
                            recProgressBar.setWidth(0);
                            lblProgress.setText("Progress: 0.00 %");
                            lblSize.setText("0 / 0" + " Kb");
                            lblFolder.setText("No folder selected");
                            txtSelectedFiles.setText("No file selected");
                            btnCopy.setDisable(true);
                            srcFiles=null;
                            destDir = null;
                            totalFiles=0;
                            totalNumberOfCopiedFiles=0;
                        }
                    }
                });
                new Thread(task).start();

            }




        }else {

            totalFiles=returnFileList.size();
            try {




                /* Make All directories with empty */
                for (File ff : returnDirectoryList) {
                    String filePath = ff.getAbsolutePath().replace(srcFolder.getParent(), destDir.getAbsolutePath()) + "".trim();
                    File destinationFile = new File(filePath);

                    if (!destinationFile.exists()) {
                        destinationFile.mkdir();

                    }
                }

                /* Copy all files */

                for (File srfile : returnFileList) {

                    String filePath = srfile.getAbsolutePath().replace(srcFolder.getParent(), destDir.getAbsolutePath()) + "".trim();
                    File destinationFile = new File(filePath);
                    if (!destinationFile.exists()) {
                        destinationFile.createNewFile();
                    } else {

                        Optional<ButtonType> result = new Alert(Alert.AlertType.INFORMATION, "" + srfile.getName() + " File already exists. Do you want to overwrite?", ButtonType.YES, ButtonType.NO).showAndWait();
                        if (result.get() == ButtonType.NO) {
                            return;
                        }


                    }

                    var task = new Task<>() {
                        @Override
                        protected Object call() throws Exception {
                            FileInputStream fis = new FileInputStream(srfile);
                            FileOutputStream fos = new FileOutputStream(destinationFile);
                            BufferedInputStream bis = new BufferedInputStream(fis);
                            BufferedOutputStream bos = new BufferedOutputStream(fos);

                            while (true) {
                                byte[] buffer = new byte[1024 * 50];
                                int read = bis.read(buffer);

                                if (read == -1) {
                                    break;
                                }
                                bos.write(buffer, 0, read);
                                totalRead += read;
                                updateProgress(totalRead, fileSize);
                            }


                            updateProgress(fileSize, fileSize);
                            bos.close();
                            bis.close();
                            return null;
                        }
                    };
                    task.progressProperty().addListener(new ChangeListener<Number>() {
                        @Override
                        public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {

                            lblNumberOfFiles.setText("Files : " + totalNumberOfCopiedFiles + " / " + totalFiles);
                            recProgressBar.setWidth((recContainer.getWidth() / fileSize) * totalRead);
                            lblProgress.setText("Progress: " + formatNumber2(totalRead * 1.0 / fileSize * 100) + "%");
                            lblSize.setText(formatNumber(totalRead / 1024.0) + " / " + formatNumber(fileSize / 1024.0) + " Kb");

                        }
                    });
                    task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                        @Override
                        public void handle(WorkerStateEvent workerStateEvent) {
                            totalNumberOfCopiedFiles += 1;
                            if (totalNumberOfCopiedFiles == totalFiles) {
                                recProgressBar.setWidth(recContainer.getWidth());
                                lblNumberOfFiles.setText("Files : " + totalFiles + " / " + totalFiles);
                                new Alert(Alert.AlertType.INFORMATION, "File has been copied successfully").showAndWait();
                                lblNumberOfFiles.setText("Files : 0 / 0");
                                recProgressBar.setWidth(0);
                                lblProgress.setText("Progress: 0.00 %");
                                lblSize.setText("0 / 0" + " Kb");
                                lblFolder.setText("No folder selected");
                                txtSelectedFiles.setText("No file selected");
                                btnCopy.setDisable(true);
                                returnDirectoryList=null;
                                returnFileList=null;
                                destDir = null;
                                totalFiles=0;
                                totalNumberOfCopiedFiles=0;
                            }
                        }
                    });
                    new Thread(task).start();

                }
            }catch (Exception e){
                System.out.println(e);
            }

        }
        rdoFiles.setDisable(false);
        rdoFolders.setDisable(false);

    }

    public void rdoFilesOnAction(ActionEvent actionEvent) {
        rdoFolders.setSelected(false);
        rdoFiles.setSelected(true);
        try{
            txtSelectedFiles.clear();
            txtSelectedFiles.setText("No Selected Files");
            returnFileList.clear();
            srcFiles.clear();
            returnDirectoryList.clear();

        }catch(Exception e){

        }


    }

    public void rdoFoldersOnAction(ActionEvent actionEvent) {
        rdoFiles.setSelected(false);
        rdoFolders.setSelected(true);
        try{
            txtSelectedFiles.clear();
            txtSelectedFiles.setText("No Selected Files");
            returnFileList.clear();
            srcFiles.clear();
            returnDirectoryList.clear();

        }catch(Exception e){

        }

    }
    public void getFiles(File srcFolder){

        File[] files = srcFolder.listFiles();
        if (files.length==0){
            returnDirectoryList.add(srcFolder);   //get all empty directories
        }else{
            for(File f: files){
                if(f.isDirectory()){
                    getFiles(f);


                }else if(f.isFile()){
                    File file = new File(f.getParentFile().getAbsolutePath());
                    returnDirectoryList.add(file); //get  directories of file f
                    returnFileList.add(f); //get all files
                }
            }

        }
    }
    private String formatNumber(double input){

        NumberFormat ni =NumberFormat.getNumberInstance();
        ni.setGroupingUsed(true);
        ni.setMaximumFractionDigits(3);
        ni.setMinimumFractionDigits(3);
        String format = ni.format(input);
        return format;

    }
    private String formatNumber2(double input){

        NumberFormat ni =NumberFormat.getNumberInstance();
        ni.setMaximumFractionDigits(2);
        ni.setMinimumFractionDigits(2);
        String format = ni.format(input);
        return format;

    }


}
