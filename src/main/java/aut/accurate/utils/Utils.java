package aut.accurate.utils;

import io.github.cdimascio.dotenv.Dotenv;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;

import static aut.accurate.utils.Constants.ROOT;

public class Utils {

    public static final Dotenv env = Dotenv.load();
    public static Properties ELEMENTS;

    public static void loadElementProperties(String directory) {
        File folder = new File(directory);
        File[] listOfFiles = folder.listFiles();
        ELEMENTS = new Properties();

        for (int i = 0; i < Objects.requireNonNull(listOfFiles).length; ++i) {
            if (listOfFiles[i].isFile() && listOfFiles[i].toString().contains(".properties")) {
                try {
                    ELEMENTS.load(Files.newInputStream(Paths.get(directory + listOfFiles[i].getName())));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String env(String env) {
        env = Utils.env.get(env.replace(" ", "_").toUpperCase());
        if (env == null) {
            printError("Env data not found! Please check your env file.");
        }
        return env;
    }

    public static void printError(String error) {
        throw new AssertionError(error);
    }

    public static String readExcel(String file, String header) {
        String data = "";
        try {
            FileInputStream fileInputStream = new FileInputStream(ROOT + file + ".xlsx");
            Workbook workbook = new XSSFWorkbook(fileInputStream);
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            Map<String, Integer> headerIndices = new HashMap<>();
            for (Cell cell : headerRow) {
                String headerName = cell.getStringCellValue();
                int columnIndex = cell.getColumnIndex();
                headerIndices.put(headerName, columnIndex);
            }

            data = getValueByHeaderName(sheet, headerIndices, header);

            workbook.close();
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    private static String getValueByHeaderName(Sheet sheet, Map<String, Integer> headerIndices, String headerName) {
        int columnIndex = headerIndices.get(headerName);
        Row dataRow = sheet.getRow(1); // Assuming data starts from the second row (index 1)
        Cell cell = dataRow.getCell(columnIndex);
        if (cell != null) {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue();
                case NUMERIC:
                    return String.valueOf(cell.getNumericCellValue());
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case BLANK:
                    return "[BLANK]";
                default:
                    return "[UNKNOWN]";
            }
        }
        return null;
    }

    public static LocalDate calculateYesterday(LocalDate currentDate, int daysToSubtract) {
        return currentDate.minusDays(daysToSubtract);
    }

    public static void uploadToFtp(String source, String destination) {
        FTPClient ftpClient = new FTPClient();
        FileInputStream fileInputStream = null;

        try {
            ftpClient.connect(env("FTP_HOST"), 21);
            ftpClient.login(env("FTP_USERNAME"), env("FTP_PASSWORD"));

            File sourceFile = new File(ROOT + "/" + source);
            fileInputStream = new FileInputStream(sourceFile);

            ftpClient.enterLocalPassiveMode();
            ftpClient.setRemoteVerificationEnabled(true);
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            boolean directoryExists = ftpClient.changeWorkingDirectory(destination);
            if (directoryExists) {
                System.out.println("The directory already exists.");
            } else {
                boolean created = ftpClient.makeDirectory(destination);
                if (created) {
                    System.out.println("The directory was created successfully.");
                    ftpClient.changeWorkingDirectory(destination);
                } else {
                    System.out.println("Failed to create the directory.");
                }
            }

            boolean checkUpload = ftpClient.storeFile(sourceFile.getName(), fileInputStream);
            if (checkUpload) {
                System.out.println("File uploaded successfully!");
            } else {
                printError("Failed upload file!");
            }

        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                ftpClient.disconnect();
            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    public static String getLatestFile(String directory) {
        File folder = new File(ROOT + "/" + directory);
        File[] files = folder.listFiles();

        String fileName = "";
        if (files != null && files.length > 0) {
            Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
            File latestFile = files[0];
            fileName = latestFile.getName();
            System.out.println("Latest file: " + fileName);
        } else {
            System.out.println("No files found in the folder.");
        }

        return fileName;
    }

    public static void uploadHtmlReport() {
        File file = new File(ROOT + "/build/result.html");
        String username = env("WEBDAV_USERNAME");
        String password = env("WEBDAV_PASSWORD");
        String url = env("WEBDAV_URL");

        try{
            HttpClient client = new HttpClient();
            Credentials credentialsWebDav = new UsernamePasswordCredentials(username, password);
            client.getState().setCredentials(AuthScope.ANY, credentialsWebDav);

            PutMethod method = new PutMethod(url + file.getName());
            RequestEntity requestEntity = new InputStreamRequestEntity(
                    Files.newInputStream(file.toPath()));
            method.setRequestEntity(requestEntity);
            client.executeMethod(method);
            System.out.println(method.getStatusCode() + " " + method.getStatusText());
        } catch(IOException e){
            e.printStackTrace();
        }
    }
}
