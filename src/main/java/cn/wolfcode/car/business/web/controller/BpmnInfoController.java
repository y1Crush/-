package cn.wolfcode.car.business.web.controller;


import cn.wolfcode.car.business.domain.BpmnInfo;
import cn.wolfcode.car.business.query.BpmnInfoQuery;
import cn.wolfcode.car.business.service.IBpmnInfoService;
import cn.wolfcode.car.common.base.page.TablePageInfo;
import cn.wolfcode.car.common.config.SystemConfig;
import cn.wolfcode.car.common.exception.BusinessException;
import cn.wolfcode.car.common.util.file.FileUploadUtils;
import cn.wolfcode.car.common.web.AjaxResult;
import org.apache.poi.util.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * 客户控制器
 */
@Controller
@RequestMapping("business/bpmnInfo")
public class BpmnInfoController {
    //模板前缀
    private static final String prefix = "business/bpmnInfo/";

    @Autowired
    private IBpmnInfoService bpmnInfoService;

    //页面------------------------------------------------------------
    //列表
    @RequiresPermissions("business:bpmnInfo:view")
    @RequestMapping("/listPage")
    public String listPage() {
        return prefix + "list";
    }

    @RequiresPermissions("business:bpmnInfo:add")
    @RequestMapping("/addPage")
    public String addPage() {
        return prefix + "add";
    }

    @RequiresPermissions("business:bpmnInfo:edit")
    @RequestMapping("/editPage")
    public String editPage(Long id, Model model) {
        model.addAttribute("bpmnInfo", bpmnInfoService.get(id));
        return prefix + "edit";
    }

    //数据-----------------------------------------------------------
    //列表
    @RequiresPermissions("business:bpmnInfo:list")
    @RequestMapping("/query")
    @ResponseBody
    public TablePageInfo<BpmnInfo> query(BpmnInfoQuery qo) {
        return bpmnInfoService.query(qo);
    }

    //新增
    @RequiresPermissions("business:bpmnInfo:add")
    @RequestMapping("/add")
    @ResponseBody
    public AjaxResult addSave(BpmnInfo bpmnInfo) {
        bpmnInfoService.save(bpmnInfo);
        return AjaxResult.success();
    }

    //编辑
    @RequiresPermissions("business:bpmnInfo:edit")
    @RequestMapping("/edit")
    @ResponseBody
    public AjaxResult edit(BpmnInfo bpmnInfo) {
        bpmnInfoService.update(bpmnInfo);
        return AjaxResult.success();
    }

    //删除
    @RequiresPermissions("business:bpmnInfo:remove")
    @RequestMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        bpmnInfoService.deleteBatch(ids);
        return AjaxResult.success();
    }

    @RequiresPermissions("business:bpmnInfo:deployPage")
    @RequestMapping("/deployPage")
    public String deployPage(Model model, Long id) {
        model.addAttribute("bpmnInfo", bpmnInfoService.get(id));
        return prefix + "deploy";
    }

    @RequiresPermissions("business:bpmnInfo:upload")
    @RequestMapping("/upload")
    @ResponseBody
    public AjaxResult upload(MultipartFile file) throws IOException {
        String filePath = "";
        if (file != null && file.getSize() > 0) {
            //判断后缀名
            String extName = FileUploadUtils.getExtension(file);
            if ("bpmn".equalsIgnoreCase(extName)) {
                filePath = FileUploadUtils.upload(SystemConfig.getUploadPath(), file);
            } else {
                throw new BusinessException("流程定义只允许上传bpmn文件");
            }
        } else {
            throw new BusinessException("流程定义不允许上传空文件");
        }
        return AjaxResult.success("上传成功",filePath);
    }
    @RequiresPermissions("business:bpmnInfo:deploy")
    @RequestMapping("/deploy")
    @ResponseBody
    public AjaxResult deploy(String path,String bpmnType,String info) throws FileNotFoundException {
        bpmnInfoService.deploy(path,bpmnType,info);
        return AjaxResult.success();
    }
    @RequiresPermissions("business:bpmnInfo:delete")
    @RequestMapping("/delete")
    @ResponseBody
    public AjaxResult delete(Long id) {
        bpmnInfoService.delete(id);
        return AjaxResult.success();
    }
    @RequiresPermissions("business:bpmnInfo:readResource")
    @RequestMapping("/readResource")
    @ResponseBody
    public void readResource(String deploymentId, String type, HttpServletResponse response) throws IOException {
        InputStream inputStream = bpmnInfoService.getResourceStream(deploymentId, type);
        IOUtils.copy(inputStream,response.getOutputStream());

    }
}
