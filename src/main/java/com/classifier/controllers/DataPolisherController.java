package com.classifier.controllers;

import com.classifier.services.DocumentElement;
import com.classifier.services.DocumentParser;
import com.classifier.services.FileConverter;
import com.classifier.services.ParsedDocument;
import com.classifier.services.PdfHandler;
import com.classifier.services.TableChunk;
import com.classifier.services.TextChunk;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api")
public class DataPolisherController {

    private final Map<String, ParsedDocument> documentStorage = new ConcurrentHashMap<>();
    private final FileConverter fileConverter = new FileConverter();
    private final DocumentParser documentParser = new DocumentParser();

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }

        try {
            // Save file to temp directory
            File tempFile = File.createTempFile("upload-", "-" + file.getOriginalFilename());
            file.transferTo(tempFile);

            // Convert and Parse
            List<String> rawLines = fileConverter.convertToCSV(tempFile);
            ParsedDocument parsedDoc = documentParser.parse(rawLines, file.getOriginalFilename());

            // Generate UUID to store document session
            String docId = UUID.randomUUID().toString();
            documentStorage.put(docId, parsedDoc);

            // Create JSON friendly response
            List<Map<String, Object>> elementsList = new ArrayList<>();
            for (DocumentElement element : parsedDoc.getElements()) {
                Map<String, Object> elemData = new HashMap<>();
                elemData.put("sequenceIndex", element.getSequenceIndex());
                if (element instanceof TableChunk) {
                    TableChunk t = (TableChunk) element;
                    elemData.put("type", "TABLE");
                    elemData.put("tableID", t.getTableID());
                    elemData.put("headers", t.getHeaders());
                    elemData.put("rows", t.getRows());
                } else if (element instanceof TextChunk) {
                    TextChunk c = (TextChunk) element;
                    elemData.put("type", "TEXT");
                    elemData.put("lines", c.getLines());
                }
                elementsList.add(elemData);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("documentId", docId);
            response.put("fileName", parsedDoc.getFileName());
            response.put("elements", elementsList);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to process file"));
        }
    }

    public static class GeneratePdfRequest {
        public String documentId;
        public List<TableConfig> tableConfigs;
    }

    public static class TableConfig {
        public String tableID;
        public boolean activated;
        public int sortColumnIndex;
        public boolean ascending;
    }

    @PostMapping("/generate-pdf")
    public ResponseEntity<Resource> generatePdf(@RequestBody GeneratePdfRequest request) {
        ParsedDocument doc = documentStorage.get(request.documentId);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, TableConfig> configMap = new HashMap<>();
        if (request.tableConfigs != null) {
            for (TableConfig tc : request.tableConfigs) {
                configMap.put(tc.tableID, tc);
            }
        }

        try {
            File pdfFile = File.createTempFile("polished-", ".pdf");
            
            // Re-apply sorting settings
            for (DocumentElement element : doc.getElements()) {
                if (element instanceof TableChunk) {
                    TableChunk t = (TableChunk) element;
                    // FORCE SELECT SO IT RENDERS IN PDF
                    t.setSelected(true);
                    
                    TableConfig conf = configMap.get(t.getTableID());
                    if (conf != null && conf.activated) {
                        t.setSortColumnIndex(conf.sortColumnIndex);
                        t.setAscending(conf.ascending);
                        t.sortData();
                    } else {
                        // Not activated, reset sort index just in case
                        t.setSortColumnIndex(-1);
                    }
                }
            }

            // Generate
            PdfHandler pdfHandler = new PdfHandler(pdfFile.getAbsolutePath());
            pdfHandler.generatePdf(doc);

            Resource resource = new FileSystemResource(pdfFile);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName() + "-polished.pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
