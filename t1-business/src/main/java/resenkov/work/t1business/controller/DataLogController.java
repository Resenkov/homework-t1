package resenkov.work.t1business.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import resenkov.work.t1business.service.DataSourceErrorLogService;
import resenkov.work.t1metricsstarter.entity.DataSourceErrorLog;

import java.util.List;

@RestController
@RequestMapping("/logs")
public class DataLogController {
    private DataSourceErrorLogService dataSourceErrorLogService;

    @GetMapping
    public ResponseEntity<List<DataSourceErrorLog>> findAll(){
        return ResponseEntity.ok(dataSourceErrorLogService.findAll());
    }
}
