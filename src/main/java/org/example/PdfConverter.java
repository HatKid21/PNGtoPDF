package org.example;

import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

public class PdfConverter {

    private Document pdf;

    private Deque<Image> imageQueue = new ArrayDeque<>();
    private static final String[] availableFormats = new String[]{".jpg",".png",".jpeg"};
    private final String pdfPath;
    private boolean documentCreated = false;

    private boolean hasIndents = false ;
    private float leftIndent;

    private float topIndent;

    public PdfConverter(String path){
        this.pdfPath = path;
    }

    private void createDocument(){
        pdf = new Document();
        try {
            PdfWriter.getInstance(pdf,new FileOutputStream(pdfPath));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean check(String path){
        for (String extension : availableFormats){
            String fileEx = "." + path.split("\\.")[1];
            if (extension.equals(fileEx.toLowerCase())){
                return true;
            }
        }
        return false;
    }

    public int getQueueLength(){
        return imageQueue.size();
    }

    public boolean removeImage(){
        if (!imageQueue.isEmpty()){
            imageQueue.removeLast();
            return true;
        }
        return false;
    }

    public boolean addImageToQueue(String path){
        if (!check(path)){
            System.out.println("HERE");
            return false;
        }
        try {
            Image image = Image.getInstance(path);
            imageQueue.add(image);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public void addImagesToDocument(){
        if (!documentCreated && pdfPath != null){
            createDocument();
            documentCreated = true;
        } else{
            System.out.println("Something went wrong");
            return;
        }

        pdf.open();

        Rectangle pageSize = pdf.getPageSize();
        float pageWidth = pageSize.getWidth();
        float pageHeight = pageSize.getHeight();

        boolean firstPage = true;

        while (!imageQueue.isEmpty()) {

            if (!firstPage){
                pdf.newPage();
            }else{
                firstPage = false;
            }

            Image image = imageQueue.poll();
            float imageWidth = image.getWidth();
            float imageHeight = image.getHeight();

            float widthScale = pageWidth / imageWidth * 0.93f;
            float heightScale = pageHeight / imageHeight * 0.93f;
            float scale = Math.min(widthScale, heightScale);

            image.scalePercent(scale * 100);
            if (hasIndents){
                image.setAbsolutePosition(leftIndent,topIndent);
            }
            pdf.add(image);
        }

        pdf.close();

    }

    public void setIndents(float leftIndent, float topIndent) {
        this.leftIndent = leftIndent * 28.346f;
        this.topIndent = topIndent * 28.346f;
        this.hasIndents = true;
    }
}
