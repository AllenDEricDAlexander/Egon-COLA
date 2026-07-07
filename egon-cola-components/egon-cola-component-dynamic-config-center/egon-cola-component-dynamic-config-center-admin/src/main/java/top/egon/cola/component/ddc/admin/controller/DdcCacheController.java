package top.egon.cola.component.ddc.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.result.Result;
import top.egon.cola.component.ddc.admin.model.vo.DdcCacheCheckRow;
import top.egon.cola.component.ddc.admin.service.DdcCacheService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ddc/cache")
public class DdcCacheController {

    private final DdcCacheService cacheService;

    public DdcCacheController(DdcCacheService cacheService) {
        this.cacheService = cacheService;
    }

    @PostMapping("/rebuild")
    public Result<Integer> rebuild(@RequestParam("appCode") String appCode,
                                   @RequestParam("env") String env,
                                   @RequestParam("namespace") String namespace) {
        return Result.success(cacheService.rebuild(appCode, env, namespace));
    }

    @GetMapping("/check")
    public Result<List<DdcCacheCheckRow>> check(@RequestParam("appCode") String appCode,
                                                @RequestParam("env") String env,
                                                @RequestParam("namespace") String namespace) {
        return Result.success(cacheService.check(appCode, env, namespace));
    }
}
