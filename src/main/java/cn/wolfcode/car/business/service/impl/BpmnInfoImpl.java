package cn.wolfcode.car.business.service.impl;

import cn.wolfcode.car.business.domain.BpmnInfo;
import cn.wolfcode.car.business.mapper.BpmnInfoMapper;
import cn.wolfcode.car.business.query.BpmnInfoQuery;
import cn.wolfcode.car.business.service.IBpmnInfoService;
import cn.wolfcode.car.common.base.page.TablePageInfo;
import cn.wolfcode.car.common.config.SystemConfig;
import cn.wolfcode.car.common.util.Convert;
import com.github.pagehelper.PageHelper;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.image.impl.DefaultProcessDiagramGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
@Transactional
public class BpmnInfoImpl implements IBpmnInfoService {

    @Autowired
    private BpmnInfoMapper bpmnInfoMapper;
    @Autowired
    private RepositoryService repositoryService;

    @Override
    public TablePageInfo<BpmnInfo> query(BpmnInfoQuery qo) {
        PageHelper.startPage(qo.getPageNum(), qo.getPageSize());
        return new TablePageInfo<BpmnInfo>(bpmnInfoMapper.selectForList(qo));
    }

    @Override
    public void save(BpmnInfo bpmnInfo) {
        bpmnInfoMapper.insert(bpmnInfo);
    }

    @Override
    public BpmnInfo get(Long id) {
        return bpmnInfoMapper.selectByPrimaryKey(id);
    }


    @Override
    public void update(BpmnInfo bpmnInfo) {

        bpmnInfoMapper.updateByPrimaryKey(bpmnInfo);
    }

    @Override
    public void deleteBatch(String ids) {
        Long[] dictIds = Convert.toLongArray(ids);
        for (Long dictId : dictIds) {
            bpmnInfoMapper.deleteByPrimaryKey(dictId);
        }
    }

    @Override
    public List<BpmnInfo> list() {
        return bpmnInfoMapper.selectAll();
    }

    @Override
    @Transactional
    public void deploy(String path, String bpmnType, String info) throws FileNotFoundException {
        //1.根据path找到流程文件
        File bpmnFile = new File(SystemConfig.getProfile(), path);
        System.out.println(bpmnFile.getPath());
        System.out.println(bpmnFile.getName());
        System.out.println(bpmnFile.getAbsoluteFile());
        //2.将流程文件部署到Activiti中
        Deployment deploy = repositoryService.createDeployment()
                .addInputStream(path, new FileInputStream(bpmnFile))
                .deploy();
        //3.根据生成部署文件 流程定义对象 封装BpmnInfo信息
        BpmnInfo bpmnInfo = new BpmnInfo();
        bpmnInfo.setBpmnType(bpmnType);//什么类型的审核
        bpmnInfo.setInfo(info);//备注信息
        bpmnInfo.setDeploymentId(deploy.getId());//部署Id
        bpmnInfo.setDeployTime(new Date());//部署时间
        //获取流程定义对象
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deploy.getId())
                .singleResult();
        bpmnInfo.setActProcessId(processDefinition.getId());
        bpmnInfo.setActProcessKey(processDefinition.getKey());
        bpmnInfo.setBpmnName(processDefinition.getName());
        //4.保存到数据库中
        bpmnInfoMapper.insert(bpmnInfo);
    }

    @Override
    public void delete(Long id) {
        //1.根据id查看bpmn对象
        BpmnInfo bpmnInfo = this.get(id);
        //2.删除bpmninfo对象
        bpmnInfoMapper.deleteByPrimaryKey(id);
        //TODO
        //查询目前这个流程定义正在运行的流程实例
        //把流程实例关联的BusinessKey找到业务对象
        //3.获取流程定义对象
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(bpmnInfo.getDeploymentId())
                .singleResult();
        //4.删除Activiti中流程定义对象
        repositoryService.deleteDeployment(bpmnInfo.getDeploymentId(), true);
        //把服务器上的文件删除
        String resourceName = processDefinition.getResourceName();
        File file = new File(SystemConfig.getProfile(),resourceName);
        if (file.exists()){
            file.delete();
        }
    }

    @Override
    public InputStream getResourceStream(String deploymentId, String type) {
        //1.获取流程定义对象
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deploymentId)
                .singleResult();
        if ("xml".equalsIgnoreCase(type)){
            //xml
            String xmlResourceName = processDefinition.getResourceName();
            //根据资源名称获取输入流
            return repositoryService.getResourceAsStream(deploymentId,xmlResourceName);
        }else {
            //png资源
            BpmnModel model = repositoryService.getBpmnModel(processDefinition.getId());
            DefaultProcessDiagramGenerator generator = new DefaultProcessDiagramGenerator();
            //generate(bpmn模型对象,需要高亮哪些节点 需要高亮的连线)
            /**
             * 需要设置字体
             * 不然会中文乱码
             */
            return generator.generateDiagram(model, Collections.EMPTY_LIST,Collections.EMPTY_LIST,
                    "宋体","宋体","宋体");
        }
    }

    @Override
    public BpmnInfo getByBpmnType(String type) {
        return bpmnInfoMapper.getByBpmnType(type);
    }
}
