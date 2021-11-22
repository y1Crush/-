package cn.wolfcode.car.business.web.controller;

import cn.wolfcode.car.base.service.IUserService;
import cn.wolfcode.car.business.domain.Statement;
import cn.wolfcode.car.business.domain.StatementItem;
import cn.wolfcode.car.business.query.StatementItemQuery;
import cn.wolfcode.car.business.service.IStatementItemService;
import cn.wolfcode.car.business.service.IStatementService;
import cn.wolfcode.car.common.base.page.TablePageInfo;
import cn.wolfcode.car.common.web.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("business/statementItem")
public class StatementItemController {
    @Autowired
    private IStatementService statementService;
    @Autowired
    private IStatementItemService statementItemService;
    @Autowired
    private IUserService userService;
    //模板前缀
    private static final String prefix = "/business/statementItem/";
    @RequestMapping("/itemDetail")
    public String itemDetail(Model model, Long statementId){
        //1.根据结算单ID查询结算单对象
        Statement statement = statementService.get(statementId);
        //判断状态
        if (Statement.STATUS_CONSUME.equals(statement.getStatus())){
            //消费中
            model.addAttribute("statement",statement);
            return prefix + "itemAdd";
        }else {
            //已支付
            statement.setPayee(userService.get(statement.getPayeeId()));
            model.addAttribute("statement",statement);
            return prefix + "itemDetail";
        }
    }
    @RequestMapping("/query")
    @ResponseBody
    public TablePageInfo<StatementItem> query(StatementItemQuery qo){
        return statementItemService.query(qo);
    }

    @RequestMapping("/saveItems")
    @ResponseBody
    public AjaxResult saveItems(@RequestBody List<StatementItem> items){
        statementItemService.saveItems(items);
        return AjaxResult.success();
    }
    @RequestMapping("/payStatement")
    @ResponseBody
    public AjaxResult payStatement(Long statementId){
        statementItemService.payStatement(statementId);
        return AjaxResult.success();
    }
}
