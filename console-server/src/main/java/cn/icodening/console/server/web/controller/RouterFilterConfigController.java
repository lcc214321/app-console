package cn.icodening.console.server.web.controller;

import cn.icodening.console.common.entity.RouterFilterConfigEntity;
import cn.icodening.console.server.service.IService;
import cn.icodening.console.server.service.RouterFilterConfigService;
import cn.icodening.console.server.web.controller.base.CrudController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author icodening
 * @date 2021.07.18
 */
@RestController
@RequestMapping("/routerFilterConfig")
public class RouterFilterConfigController implements CrudController<RouterFilterConfigEntity> {

    @Autowired
    private RouterFilterConfigService routerConfigService;

    @Override
    public IService<RouterFilterConfigEntity> getService() {
        return routerConfigService;
    }
}
