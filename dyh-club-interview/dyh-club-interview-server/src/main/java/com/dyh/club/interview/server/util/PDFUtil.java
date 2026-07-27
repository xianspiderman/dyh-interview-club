package com.dyh.club.interview.server.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Pattern;

@Slf4j
public class PDFUtil {

    private static Pattern pattern = Pattern.compile("\\s*|\t|\r|\n");

    /**
     * 获取pdf的text
     */
    public static String getPdfText(String pdfUrl) {

        PDDocument document = null;
        String text = "";
        try {
            URL url = new URL(pdfUrl);
            HttpURLConnection htpcon = (HttpURLConnection) url.openConnection();
            htpcon.setRequestMethod("GET");
            htpcon.setDoOutput(true);
            htpcon.setDoInput(true);
            htpcon.setUseCaches(false);
            htpcon.setConnectTimeout(10000);
            htpcon.setReadTimeout(10000);
            InputStream in = htpcon.getInputStream();
            document = PDDocument.load(in);
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setStartPage(0);
            stripper.setEndPage(Integer.MAX_VALUE);
            text = stripper.getText(document);
            text = pattern.matcher(text).replaceAll("");
            if (log.isInfoEnabled()) {
                log.info("识别到的pdf为{}", text);
            }
        } catch (Exception e) {
            log.error("获取pdf转为文字错误:{}", e.getMessage(), e);
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (Exception e) {
                    log.error("close error", e);
                }
            }
        }
        return text;
    }


}