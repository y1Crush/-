package cn.wolfcode.car.business.web.controller;


import cn.wolfcode.car.business.domain.CarPackageAudit;
import cn.wolfcode.car.business.query.CarPackageAuditQuery;
import cn.wolfcode.car.business.service.ICarPackageAuditService;
import cn.wolfcode.car.common.base.page.TablePageInfo;
import cn.wolfcode.car.common.web.AjaxResult;
import cn.wolfcode.car.shiro.ShiroUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

/**
 * 套餐审核记录
 */
@Controller
@RequestMapping("business/carPackageAudit")
public class CarPackageAuditController {
    //模板前缀
    private static final String prefix = "business/carPackageAudit/";

    @Autowired
    private ICarPackageAuditService carPackageAuditService;

    //页面------------------------------------------------------------
    //列表
    @RequiresPermissions("business:carPackageAudit:view")
    @RequestMapping("/listPage")
    public String listPage() {
        return prefix + "list";
    }

    @RequiresPermissions("business:carPackageAudit:add")
    @RequestMapping("/addPage")
    public String addPage() {
        return prefix + "add";
    }

    @RequiresPermissions("business:carPackageAudit:edit")
    @RequestMapping("/editPage")
    public String editPage(Long id, Model model) {
        model.addAttribute("carPackageAudit", carPackageAuditService.get(id));
        return prefix + "edit";
    }
    @RequestMapping("/todoPage")
    public String todoPage(Long id, Model model) {
        model.addAttribute("carPackageAudit", carPackageAuditService.get(id));
        return prefix + "todoPage";
    }

    //数据-----------------------------------------------------------
    //列表
    @RequiresPermissions("business:carPackageAudit:list")
    @RequestMapping("/query")
    @ResponseBody
    public TablePageInfo<CarPackageAudit> query(CarPackageAuditQuery qo) {
        return carPackageAuditService.query(qo);
    }

    @RequestMapping("/todoQuery")
    @ResponseBody
    public TablePageInfo<CarPackageAudit> todoQuery(CarPackageAuditQuery qo) {
        qo.setAuditorId(ShiroUtils.getUserId());//当登录用户作为条件
        qo.setStatus(CarPackageAudit.STATUS_IN_ROGRESS);//
        return carPackageAuditService.query(qo);
    }

    //新增
    @RequiresPermissions("business:carPackageAudit:add")
    @RequestMapping("/add")
    @ResponseBody
    public AjaxResult addSave(CarPackageAudit carPackageAudit) {
        carPackageAuditService.save(carPackageAudit);
        return AjaxResult.success();
    }

    //编辑
    @RequiresPermissions("business:carPackageAudit:edit")
    @RequestMapping("/edit")
    @ResponseBody
    public AjaxResult edit(CarPackageAudit carPackageAudit) {
        carPackageAuditService.update(carPackageAudit);
        return AjaxResult.success();
    }

    //删除
    @RequiresPermissions("business:carPackageAudit:remove")
    @RequestMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        carPackageAuditService.deleteBatch(ids);
        return AjaxResult.success();
    }

    //查看进度流程
    @RequestMapping("/processImg")
    @ResponseBody
    public void processImg(Long id, HttpServletResponse response) throws IOException {
        InputStream inputStream = carPackageAuditService.getProcessImgAsStream(id);
        IOUtils.copy(inputStream, response.getOutputStream());
    }
    //撤销
    @RequiresPermissions("business:carPackageAudit:cancelApply")
    @RequestMapping("/cancelApply")
    @ResponseBody
    public AjaxResult cancelApply(Long id) {
        carPackageAuditService.cancelApply(id);
        return AjaxResult.success();
    }
    //审批
    @RequestMapping("/auditPage")
    public String auditPage(Long id, Model model) {
        model.addAttribute("id",id);
        return prefix + "auditPage";
    }
    @RequestMapping("/audit")
    @ResponseBody
    public AjaxResult audit(Long id, Integer auditStatus,String info) {
        carPackageAuditService.audit(id,auditStatus,info);
        return AjaxResult.success();
    }

    @RequestMapping("/donePage")
    public String donePage(Long id,Model model){
        return prefix + "donePage";
    }

    @RequestMapping("/doneQuery")
    @ResponseBody
    public TablePageInfo<CarPackageAudit> toneQuery(CarPackageAuditQuery qo) {
        qo.setAuditorName(ShiroUtils.getUser().getUserName());
        return carPackageAuditService.query(qo);
    }
}
