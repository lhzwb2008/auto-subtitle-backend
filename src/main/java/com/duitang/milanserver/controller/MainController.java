package com.duitang.milanserver.controller;

import com.duitang.milanserver.model.ReturnObj;
import com.duitang.milanserver.service.ProcessLfasr;
import com.duitang.milanserver.storage.StorageFileNotFoundException;
import com.duitang.milanserver.storage.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@CrossOrigin
@RestController
public class MainController {

    private final ProcessLfasr processLfasr;

    private final StorageService storageService;

    @Autowired
    public MainController(StorageService storageService,ProcessLfasr processLfasr) {
        this.storageService = storageService;
        this.processLfasr = processLfasr;
    }


    @RequestMapping("/hello")
    public String index() {
        return "Hello World!";
    }

    @RequestMapping("/process")
    public void process(@RequestParam(value = "name", defaultValue = "World") String name,
                        @RequestParam(value = "file", defaultValue = "World") String file) {
        processLfasr.process(name);
    }




    @GetMapping("/table/list")
    public List<ReturnObj> listUploadedFiles(Model model) throws IOException {

        List<ReturnObj> returnList = new ArrayList<>();
        storageService.loadAll()
                .filter(path -> path.getFileName().toString().contains("srt"))
                .forEach(path -> returnList.add(new ReturnObj(path.getFileName().toString(),MvcUriComponentsBuilder.fromMethodName(MainController.class,
                        "serveFile", path.getFileName().toString()).build().toString())));
        return returnList;
    }

    @GetMapping("/files/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {

        Resource file = storageService.loadAsResource(filename);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getFilename() + "\"").body(file);
    }

    @PostMapping("/")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes) {

        String path = storageService.store(file);
        processLfasr.process(path);
        redirectAttributes.addFlashAttribute("message",
                "You successfully uploaded " + file.getOriginalFilename() + "!");

        return "success";
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }

}
