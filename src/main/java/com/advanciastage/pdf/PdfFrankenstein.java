package com.advanciastage.pdf;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.FilteredTextRenderListener;
import com.itextpdf.text.pdf.parser.LocationTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import com.itextpdf.text.pdf.parser.RegionTextRenderFilter;
import com.itextpdf.text.pdf.parser.RenderFilter;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;

public class PdfFrankenstein {

	public static final File INDIR = new File(getJarDirectory() + "/pdf_files/");
	public static final File OUTDIR = new File(getJarDirectory() + "/txt_files/");
	public static final File OUTEXC = new File(getJarDirectory() + "/exc/");
	public static final File MOVED = new File(getJarDirectory() + "/giusti/");

	public static final String FULL_NAME_REGEX = "^[A-Za-zÀ-ÿ'’.:]+(?: [A-Za-zÀ-ÿ'’.:]+)*$";
	public static final String EMAIL_REGEX = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
	public static final String TEL_REGEX = "^(?:\\+?\\d{1,4}[ -]?|\\(?\\d{1,4}\\)?[ -]?)(?:\\d{1,4}[ -]?)*\\d{1,4}$";

	public static final List<String> LINES_TO_REMOVE = List.of("CONTATTI", "C O N T", "Ho", "program", "• ",
			"lavorando", "migliorato", "scrum", "Unit of", "Junior", "J A V A", "Java", "J U N I O R", "unior",
			"JQuery", "relazion", "Macomer", "Via", "J unior", "istruz", "ambito", "FORMAZIONE", "rmazione",
			"ormazione", "str.", "Gener", "FFOO", "F O R M", "FO R", "Pazienza", "Corso", "D igit", "Archi", "FOT",
			"appli", "FTO", "INFO", "Latina", "Roma", "voc", "piove", "organizzato", "G E N E R", "Appreso", "HTML",
			"inoltre");

	public static final List<String> LINKS_TO_REMOVE = List.of("http", "github", "link", ".www", "www.", "About",
			"profilo", "Durante", "il corso", "attiv", "maora", "PROFILO");

	public static void main(String[] args) throws IOException {
		Date start = new Date();
		System.out.println("Start: " + start);

		createDirectoryIfNotExists(OUTDIR.getAbsolutePath());
		createDirectoryIfNotExists(OUTEXC.getAbsolutePath());
		createDirectoryIfNotExists(MOVED.getAbsolutePath());

		pdfLoop();
		System.out.println();
		createExc();
		System.out.println();

		moveFiles();
		System.out.println();

		try {
			deleteDirectory(OUTDIR.toPath());
			System.out.println("Directory txt_files deleted successfully.");
		} catch (IOException e) {
			e.printStackTrace();
		}

		Date end = new Date();
		System.out.println("End: " + end);
		System.out.println("Elapsed time: " + (end.getTime() - start.getTime() + "ms"));
	}

	public static void parsePdf(float x, float y, float width, float height, String pdf, String txt)
			throws IOException {
		Rectangle rect = new Rectangle(x, y, width, height);
		RenderFilter filter = new RegionTextRenderFilter(rect);
		PdfReader reader = null;

		try (PrintWriter out = new PrintWriter(new FileOutputStream(txt))) {
			reader = new PdfReader(pdf);
			TextExtractionStrategy strategy;
			for (int i = 1; i <= reader.getNumberOfPages(); i++) {
				strategy = new FilteredTextRenderListener(new LocationTextExtractionStrategy(), filter);
				out.println(PdfTextExtractor.getTextFromPage(reader, i, strategy));
			}
		} finally {
			if (reader != null) {
				reader.close();
			}
			System.gc();
		}
	}

	public static void cutPdf(List<String> lineToRemove, List<String> linksToRemoveEnd, String upd, String flt,
			Boolean append)
			throws IOException {
		String fileName = new File(upd).getName();
		String outTxt = flt + fileName.replace("-upd.txt", "-flt.txt");

		try (BufferedReader reader = new BufferedReader(new FileReader(upd));
				BufferedReader readerNextLine = new BufferedReader(new FileReader(upd));
				BufferedWriter writer = new BufferedWriter(new FileWriter(outTxt, append))) {

			String currentLine;
			String nextLine;
			String tempLine;
			String emailBig;

			readerNextLine.readLine();

			Integer count = 0;

			while ((currentLine = reader.readLine()) != null) {
				String trimmedLine = currentLine.trim();

				if (trimmedLine.isBlank() || trimmedLine.length() == 1
						|| isValidDate(trimmedLine.replaceAll("\\s+", "").replaceAll("/\0/g", ""))
						|| trimmedLine.contains("gennaio")) {
					nextLine = readerNextLine.readLine();
					count++;
					continue;
				}

				if (lineToRemove.stream().map(String::toUpperCase).anyMatch(trimmedLine.toUpperCase()::startsWith)) {
					nextLine = readerNextLine.readLine();
					count++;
					continue;
				}

				if (trimmedLine.equals("+39")) {
					nextLine = readerNextLine.readLine();
					count++;
					continue;
				}

				if (trimmedLine.startsWith("39-") || trimmedLine.matches(TEL_REGEX)) {
					tempLine = trimmedLine.replace("-", "");
					writer.write(tempLine.trim() + System.getProperty("line.separator"));
					nextLine = readerNextLine.readLine();
					count++;
					continue;
				}

				if ((emailBig = trimmedLine.replaceAll("\\s+", "")).matches(EMAIL_REGEX)) {
					if ((nextLine = readerNextLine.readLine()) != null) {
						if (nextLine.matches(TEL_REGEX)) {
							writer.write(nextLine.trim() + System.getProperty("line.separator") + emailBig);
							break;
						}
					} else {
						writer.write(emailBig + System.getProperty("line.separator"));
						break;
					}
				}

				if ((nextLine = readerNextLine.readLine()) != null) {
					String tel = trimmedLine.trim() + nextLine.trim();

					if (tel.matches(TEL_REGEX)) {
						writer.write(tel + System.getProperty("line.separator"));
						currentLine = reader.readLine();
						nextLine = readerNextLine.readLine();
						count++;
						continue;
					}

					String email = trimmedLine.trim() + nextLine.trim();

					if ((emailBig = email.replaceAll("\\s+", "")).matches(EMAIL_REGEX)) {
						writer.write(emailBig + System.getProperty("line.separator"));
						break;
					}
				}

				if (linksToRemoveEnd.stream().map(String::toUpperCase)
						.anyMatch(trimmedLine.toUpperCase()::startsWith)) {
					break;
				}
				writer.write(trimmedLine.trim() + System.getProperty("line.separator"));

				count++;
			}
		}
	}

	public static void fixPdf(String flt, String fix) throws IOException {
		String fileName = new File(flt).getName();
		String outTxt = fix + fileName.replace("-flt.txt", "-fix.txt");

		try (BufferedReader reader = new BufferedReader(new FileReader(flt));
				BufferedReader readerNextLine = new BufferedReader(new FileReader(flt));
				BufferedWriter writer = new BufferedWriter(new FileWriter(outTxt))) {

			String currentLine;
			String nextLine;

			String firstLine = readerNextLine.readLine();

			Integer count = 0;

			while ((currentLine = reader.readLine()) != null && count < 3) {

				String trimmedLine = currentLine.trim().replace("Nome completo: ", "");

				if (!trimmedLine.replaceAll(" +", " ").matches(FULL_NAME_REGEX) && count == 0) {
					writer.write(System.getProperty("line.separator"));
				}

				if ((nextLine = readerNextLine.readLine()) != null && count == 0) {
					if (nextLine.trim().matches(FULL_NAME_REGEX)) {
						String fullName = trimmedLine + nextLine;
						writer.write(fullName + System.getProperty("line.separator"));
						currentLine = reader.readLine();
						count++;
						continue;
					}
				}

				if (count > 0 && trimmedLine.equals(firstLine)) {
					nextLine = readerNextLine.readLine();
					continue;
				}

				switch (getSuffix(trimmedLine)) {
					case "hotma":
						writer.write(trimmedLine + "il.it");
						break;
					case "gmail":
						writer.write(trimmedLine + ".com");
						break;
					case "gmail.":
						writer.write(trimmedLine + "com");
						break;
					case "gmai":
						writer.write(trimmedLine + "l.com");
						break;
					case ".c":
						writer.write(trimmedLine + "om");
						break;
					case ".co":
						writer.write(trimmedLine.replaceAll("\\s+", "") + "m");
						break;
					case "proton.m":
						writer.write(trimmedLine + "e");
						break;
				}

				if (trimmedLine.replaceAll("\\s+", "").matches(EMAIL_REGEX)) {
					writer.write(trimmedLine.replaceAll("\\s+", ""));
					break;
				}

				if (count == 2 && !trimmedLine.matches(EMAIL_REGEX)) {
					writer.write(System.getProperty("line.separator"));
					break;
				}

				writer.write(trimmedLine + System.getProperty("line.separator"));

				count++;
			}
		}
	}

	private static String getSuffix(String line) {
		if (line.endsWith("hotma"))
			return "hotma";
		if (line.endsWith("gmail"))
			return "gmail";
		if (line.endsWith("gmail."))
			return "gmail.";
		if (line.endsWith("gmai"))
			return "gmai";
		if (line.endsWith(".c"))
			return ".c";
		if (line.endsWith(".co"))
			return ".co";
		if (line.endsWith("proton.m"))
			return "proton.m";
		return "";
	}

	public static void pdfLoop() throws IOException {
		File dir = INDIR;
		File[] directoryListing = dir.listFiles();

		if (directoryListing != null) {
			for (File child : directoryListing) {
				if (child.isFile() && child.getName().endsWith(".pdf")) {
					try {
						String pdfFilePath = child.getAbsolutePath();
						System.out.println("Parsing file: " + pdfFilePath);

						String outputFilePath = OUTDIR + "/" + child.getName().replace(".pdf", "-upd.txt");
						String outputFixedFilePath = outputFilePath.replace("-upd.txt", "-flt.txt");

						// first name and last name
						parsePdf(180f, 650f, 1900f, 2500f, pdfFilePath, outputFilePath);

						cutPdf(LINES_TO_REMOVE, LINKS_TO_REMOVE, outputFilePath, OUTDIR + "/", false);

						// contact info
						parsePdf(10f, 400f, 170f, 650f, pdfFilePath, outputFilePath);

						cutPdf(LINES_TO_REMOVE, LINKS_TO_REMOVE, outputFilePath, OUTDIR + "/", true);

						fixPdf(outputFixedFilePath, OUTDIR + "/");

					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		} else {
			System.err.println("Directory pdf_files does not contain files.");
		}
	}

	public static void createExc() throws IOException {
		File dir = OUTDIR;
		File[] directoryListing = dir.listFiles();

		try (Workbook workbook = new HSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("Gen_Italy");
			Sheet errSheet = workbook.createSheet("Errati");
			int rowIndex = 0;
			int errRowIndex = 0;

			if (directoryListing != null) {
				for (File child : directoryListing) {
					if (child.isFile() && child.getName().endsWith("-fix.txt")) {
						String txtFilePath = child.getAbsolutePath();
						System.out.println("Parsing file: " + txtFilePath);

						String currentLine;
						String email = null;
						String firstField = null;
						String secondField = null;

						Integer count = 0;

						try (BufferedReader reader = new BufferedReader(new FileReader(child))) {
							while ((currentLine = reader.readLine()) != null) {
								if (currentLine.matches(EMAIL_REGEX)) {
									if (count == 1) {
										Row row = errSheet.createRow(rowIndex++);
										row.createCell(0).setCellValue("");
									}
									if (count == 2) {
										email = currentLine;
									}
								} else {
									if (count == 0) {
										firstField = currentLine;
									} else if (count == 1) {
										secondField = currentLine;
									}
									count++;
								}
							}
						}
						if ((firstField != null && !firstField.isEmpty() && !firstField.isBlank())
								&& (secondField != null && !secondField.isEmpty() && !secondField.isBlank())
								&& (email != null && email.matches(EMAIL_REGEX))) {
							Row row = sheet.createRow(rowIndex++);
							row.createCell(0).setCellValue(firstField);
							row.createCell(1).setCellValue(secondField);
							row.createCell(2).setCellValue(email);
						} else {
							Row row = errSheet.createRow(errRowIndex++);
							row.createCell(0)
									.setCellValue((firstField != null && !firstField.isEmpty() && !firstField.isBlank())
											? firstField
											: "");
							row.createCell(1).setCellValue(
									(secondField != null && !secondField.isEmpty() && !secondField.isBlank())
											? secondField
											: "");
							row.createCell(2).setCellValue(email != null ? email : "");
						}
					}
				}
				try (FileOutputStream writer = new FileOutputStream(OUTEXC + "/Gen_Italy.xls")) {
					workbook.write(writer);
				}
			} else {
				System.err.println("Directory txt_files does not contain files.");
			}
		}
	}

	public static boolean isValidDate(String inDate) {

		List<String> formatStrings = Arrays.asList("dd/MM/yyyy", "dd MMMM yyyy", "dd-MM-yyyy");

		for (String format : formatStrings) {

			try {
				SimpleDateFormat dateFormat = new SimpleDateFormat(format);
				dateFormat.setLenient(false);
				dateFormat.parse(inDate);
				return true;
			} catch (Exception pe) {

			}
		}
		return false;
	}

	private static void createDirectoryIfNotExists(String directoryPath) throws IOException {
		File dir = new File(directoryPath);
		if (!dir.exists()) {
			if (dir.mkdirs()) {
				System.out.println("Created directory: " + directoryPath);
			} else {
				throw new IOException("Cannot create directory: " + directoryPath);
			}
		}
	}

	private static String getJarDirectory() {
		try {
			return new File(PdfFrankenstein.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())
					.getParent();
		} catch (Exception e) {
			throw new RuntimeException("Error retrieving JAR path", e);
		}
	}

	public static boolean validFile(File inputFile) throws IOException {
		try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
				BufferedReader readerNextLine = new BufferedReader(new FileReader(inputFile))) {
			String currentLine;
			String nextLine = readerNextLine.readLine();
			Integer count = 0;

			while ((currentLine = reader.readLine()) != null && count < 3) {
				if (currentLine.isBlank() || currentLine.isEmpty() || currentLine == null) {
					return false;
				}

				nextLine = readerNextLine.readLine();
				if (count == 1 && (nextLine == null || nextLine.isEmpty() || nextLine.isBlank())) {
					return false;
				}
				if (count == 2 && currentLine != null && !currentLine.matches(EMAIL_REGEX)) {
					return false;
				}
				count++;
			}
			if (count == 0) {
				return false;
			}
		}
		return true;
	}

	public static void moveFiles() throws IOException {

		File dir = OUTDIR;
		File[] directoryListing = dir.listFiles();

		System.out.println("Input Directory: " + INDIR.getAbsolutePath());
		System.out.println("Output Directory: " + MOVED.getAbsolutePath());

		if (directoryListing != null) {
			for (File child : directoryListing) {
				if (child.isFile() && child.getName().endsWith("-fix.txt")) {
					String fixFileName = child.getName();
					String pdfFileName = fixFileName.replace("-fix.txt", ".pdf");
					Path pdfFilePath = Paths.get(INDIR.getAbsolutePath(), pdfFileName);
					Path movedFilePath = Paths.get(MOVED.getAbsolutePath(), pdfFileName);

					System.out.println("Checking for file: " + pdfFilePath);

					if (Files.exists(pdfFilePath)) {
						System.out.println("File exists: " + pdfFilePath);

						try {
							if (validFile(child)) {
								Files.move(pdfFilePath, movedFilePath, StandardCopyOption.REPLACE_EXISTING);

								System.out.println("Moved: " + pdfFileName);
							} else {
								System.out.println("Invalid file: " + fixFileName);
							}
						} catch (IOException e) {
							e.printStackTrace();
							System.out.println("Error moving or copying file: " + pdfFilePath + " - " + e.getMessage());
						}
					} else {
						System.out.println("File does not exist: " + pdfFilePath);
					}
				}
			}
		}
	}

	public static void deleteDirectory(Path path) throws IOException {
		Files.walk(path)
				.sorted(Comparator.reverseOrder())
				.map(Path::toFile)
				.forEach(file -> {
					if (!file.delete()) {
						System.err.println("Cannot delete file or directory: " + file.getAbsolutePath());
					}
				});
	}
}
