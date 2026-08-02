package com.ruoyi.wms.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.constant.HttpStatus;
import com.ruoyi.common.core.constant.ServiceConstants;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.exception.base.BaseException;
import com.ruoyi.common.core.utils.MapstructUtils;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.mybatis.core.domain.BaseEntity;
import com.ruoyi.common.mybatis.core.page.PageQuery;
import com.ruoyi.common.mybatis.core.page.TableDataInfo;
import com.ruoyi.wms.domain.bo.ReceiptOrderBo;
import com.ruoyi.wms.domain.bo.ReceiptOrderDetailBo;
import com.ruoyi.wms.domain.entity.ReceiptOrder;
import com.ruoyi.wms.domain.entity.ReceiptOrderDetail;
import com.ruoyi.wms.domain.vo.ReceiptOrderDetailVo;
import com.ruoyi.wms.domain.vo.ReceiptOrderVo;
import com.ruoyi.wms.mapper.ReceiptOrderMapper;
import com.ruoyi.wms.rcs.RcsTaskHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 入库单Service业务层处理
 *
 * @author zcc
 * @date 2024-07-19
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ReceiptOrderService {

    private final ReceiptOrderMapper receiptOrderMapper;
    private final ReceiptOrderDetailService receiptOrderDetailService;
    private final InventoryService inventoryService;
    private final InventoryHistoryService inventoryHistoryService;
    private final RcsTaskHelper rcsTaskHelper;
    private final LocationService locationService;

    /**
     * 查询入库单
     */
    public ReceiptOrderVo queryById(Long id){
        ReceiptOrderVo receiptOrderVo = receiptOrderMapper.selectVoById(id);
        Assert.notNull(receiptOrderVo, "入库单不存在");
        receiptOrderVo.setDetails(receiptOrderDetailService.queryByReceiptOrderId(id));
        return receiptOrderVo;
    }

    public Long queryIdByOrderNo(String orderNo){
        ReceiptOrderVo receiptOrderVo = receiptOrderMapper.selectVoOne(new QueryWrapper<ReceiptOrder>().eq("order_no",orderNo));
        return receiptOrderVo != null ? receiptOrderVo.getId() : null;
    }

    /**
     * 查询入库单列表
     */
    public TableDataInfo<ReceiptOrderVo> queryPageList(ReceiptOrderBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ReceiptOrder> lqw = buildQueryWrapper(bo);
        Page<ReceiptOrderVo> result = receiptOrderMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询入库单列表
     */
    public List<ReceiptOrderVo> queryList(ReceiptOrderBo bo) {
        LambdaQueryWrapper<ReceiptOrder> lqw = buildQueryWrapper(bo);
        return receiptOrderMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<ReceiptOrder> buildQueryWrapper(ReceiptOrderBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<ReceiptOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getOrderNo()), ReceiptOrder::getOrderNo, bo.getOrderNo());
        lqw.eq(bo.getOptType() != null, ReceiptOrder::getOptType, bo.getOptType());
        lqw.eq(bo.getMerchantId() != null, ReceiptOrder::getMerchantId, bo.getMerchantId());
        lqw.eq(StringUtils.isNotBlank(bo.getOrderNo()), ReceiptOrder::getOrderNo, bo.getOrderNo());
        lqw.eq(bo.getTotalAmount() != null, ReceiptOrder::getTotalAmount, bo.getTotalAmount());
        lqw.eq(bo.getOrderStatus() != null, ReceiptOrder::getOrderStatus, bo.getOrderStatus());
        lqw.orderByDesc(BaseEntity::getCreateTime);
        return lqw;
    }

    /**
     * 暂存入库单
     */
    @Transactional
    public Long insertByBo(ReceiptOrderBo bo) {
        // 校验入库单号唯一性
        validateReceiptOrderNo(bo.getOrderNo());
        // 创建入库单
        ReceiptOrder add = MapstructUtils.convert(bo, ReceiptOrder.class);
        receiptOrderMapper.insert(add);
        bo.setId(add.getId());
        List<ReceiptOrderDetailBo> detailBoList = bo.getDetails();
        List<ReceiptOrderDetail> addDetailList = MapstructUtils.convert(detailBoList, ReceiptOrderDetail.class);
        addDetailList.forEach(it -> {
            it.setOrderId(add.getId());
        });
        // 创建入库单明细
        receiptOrderDetailService.saveDetails(addDetailList);
        return add.getId();
    }

    /**
     * 入库：
     * 1.校验
     * 2.保存入库单和入库单明细
     * 3.保存库存明细
     * 4.增加库存
     * 5.保存库存记录
     */
    @Transactional
    public void receive(ReceiptOrderBo bo) {
        // 1. 校验
        validateBeforeReceive(bo);

        // 1.1 入库单状态设为"入库中"，等AGV回调完成后才更新为"已完成"
        bo.setOrderStatus(ServiceConstants.ReceiptOrderStatus.IN_PROGRESS);

        // 1.5 确保每条明细都有容器号（前端未生成时后端自动补全）
        bo.getDetails().forEach(detail -> {
            if (StringUtils.isBlank(detail.getContainerNo())) {
                String generated = locationService.generateContainerNo();
                detail.setContainerNo(generated);
                log.info("入库单 {} 明细容器号为空，自动生成：{}", bo.getOrderNo(), generated);
            }
        });

        // 2. 保存入库单和入库单明细
        if (Objects.isNull(bo.getId())) {
            insertByBo(bo);
        } else {
            updateByBo(bo);
        }

        // 3.增加库存
        inventoryService.add(bo.getDetails());

        // 4.更新库位状态和容器号
        bo.getDetails().forEach(detail -> {
            if (StringUtils.isNotBlank(detail.getSourceLocation())) {
                // 起点库位清空容器
                locationService.updateStatusAndContainerByCode(detail.getSourceLocation(), 0, null);
            }
            if (StringUtils.isNotBlank(detail.getTargetLocation())) {
                // 终点库位记录容器号
                locationService.updateStatusAndContainerByCode(
                    detail.getTargetLocation(), 1, detail.getContainerNo());
            }
        });

        // 5.保存库存记录
        inventoryHistoryService.saveInventoryHistory(bo,ServiceConstants.InventoryHistoryOrderType.RECEIPT,true);

        // 6.下发 RCS 入库任务（容错：失败不回滚库存）
        rcsTaskHelper.dispatchRcsTask(bo.getOrderNo(), RcsTaskHelper.TASK_TYPE_INBOUND, bo.getDetails());
    }

    private void validateBeforeReceive(ReceiptOrderBo bo) {
        if (CollUtil.isEmpty(bo.getDetails())) {
            throw new BaseException("商品明细不能为空");
        }
    }

    /**
     * 自动完成下一个相同起点的暂存入库单。
     * <p>
     * 当同一 taskId 收到 3 条 AGV 回调日志后触发，
     * 查找 orderStatus=0（暂存）且明细 sourceLocation 与传入值匹配的入库单，
     * 执行入库完成（增加库存、更新库位状态、保存库存记录、下发 RCS 任务）。
     *
     * @param sourceLocation 起点库位（如 R1、R2），用于匹配暂存入库单
     */
    @Transactional
    public void autoReceiveNextPendingOrder(String sourceLocation) {
        if (StringUtils.isBlank(sourceLocation)) {
            log.warn("起点为空，无法匹配暂存入库单");
            return;
        }

        // 查询所有暂存的入库单（orderStatus=0），按创建时间升序
        LambdaQueryWrapper<ReceiptOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(ReceiptOrder::getOrderStatus, ServiceConstants.ReceiptOrderStatus.PENDING);
        lqw.orderByAsc(BaseEntity::getCreateTime);
        List<ReceiptOrderVo> list = receiptOrderMapper.selectVoList(lqw);

        if (list.isEmpty()) {
            log.info("没有暂存的入库单，无需自动入库");
            return;
        }

        // 找到第一个明细 sourceLocation 匹配的入库单
        ReceiptOrderVo matchedVo = null;
        for (ReceiptOrderVo vo : list) {
            List<ReceiptOrderDetailVo> details = receiptOrderDetailService.queryByReceiptOrderId(vo.getId());
            if (details != null && !details.isEmpty()) {
                for (ReceiptOrderDetailVo detail : details) {
                    if (sourceLocation.equals(detail.getSourceLocation())) {
                        matchedVo = vo;
                        break;
                    }
                }
            }
            if (matchedVo != null) {
                break;
            }
        }

        if (matchedVo == null) {
            log.info("没有起点为 {} 的暂存入库单", sourceLocation);
            return;
        }

        log.info("自动完成入库单：{}，起点={}", matchedVo.getOrderNo(), sourceLocation);

        // 构造 Bo
        ReceiptOrderBo bo = new ReceiptOrderBo();
        bo.setId(matchedVo.getId());
        bo.setOrderNo(matchedVo.getOrderNo());
        bo.setOrderStatus(ServiceConstants.ReceiptOrderStatus.IN_PROGRESS);
        bo.setOptType(matchedVo.getOptType());
        bo.setBizOrderNo(matchedVo.getBizOrderNo());
        bo.setMerchantId(matchedVo.getMerchantId());
        bo.setWarehouseId(matchedVo.getWarehouseId());
        bo.setTotalQuantity(matchedVo.getTotalQuantity());
        bo.setTotalAmount(matchedVo.getTotalAmount());
        bo.setRemark(matchedVo.getRemark());

        // 查询明细并转换为 Bo
        List<ReceiptOrderDetailVo> detailVos = receiptOrderDetailService.queryByReceiptOrderId(matchedVo.getId());
        List<ReceiptOrderDetailBo> detailBos = new ArrayList<>();
        for (ReceiptOrderDetailVo detailVo : detailVos) {
            ReceiptOrderDetailBo detailBo = new ReceiptOrderDetailBo();
            detailBo.setId(detailVo.getId());
            detailBo.setOrderId(detailVo.getOrderId());
            detailBo.setSkuId(detailVo.getSkuId());
            detailBo.setQuantity(detailVo.getQuantity());
            detailBo.setAmount(detailVo.getAmount());
            detailBo.setRemark(detailVo.getRemark());
            detailBo.setWarehouseId(detailVo.getWarehouseId());
            detailBo.setContainerNo(detailVo.getContainerNo());
            detailBo.setSourceLocation(detailVo.getSourceLocation());
            detailBo.setTargetLocation(detailVo.getTargetLocation());
            detailBos.add(detailBo);
        }
        bo.setDetails(detailBos);

        // 执行入库
        receive(bo);
        log.info("自动完成入库单 {} 成功", matchedVo.getOrderNo());
    }

    /**
     * 修改入库单
     */
    @Transactional
    public void updateByBo(ReceiptOrderBo bo) {
        // 更新入库单
        ReceiptOrder update = MapstructUtils.convert(bo, ReceiptOrder.class);
        receiptOrderMapper.updateById(update);
        // 保存入库单明细
        List<ReceiptOrderDetail> detailList = MapstructUtils.convert(bo.getDetails(), ReceiptOrderDetail.class);
        //需要考虑detail删除
        List<ReceiptOrderDetailVo> dbList = receiptOrderDetailService.queryByReceiptOrderId(bo.getId());
        Set<Long> ids = detailList.stream().filter(it -> it.getId() != null).map(it -> it.getId()).collect(Collectors.toSet());
        List<ReceiptOrderDetailVo> delList = dbList.stream().filter(it -> !ids.contains(it.getId())).collect(Collectors.toList());
        if (CollectionUtil.isNotEmpty(delList)) {
            receiptOrderDetailService.deleteByIds(delList.stream().map(it->it.getId()).collect(Collectors.toList()));
        }
        detailList.forEach(it -> it.setOrderId(bo.getId()));
        receiptOrderDetailService.saveDetails(detailList);
    }

    /**
     * 入库单作废
     * @param id
     */
    public void editToInvalid(Long id) {
        LambdaUpdateWrapper<ReceiptOrder> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(ReceiptOrder::getId, id);
        wrapper.set(ReceiptOrder::getOrderStatus, ServiceConstants.ReceiptOrderStatus.INVALID);
        receiptOrderMapper.update(null, wrapper);
    }

    /**
     * 将入库单标记为已完成（AGV回调完成后调用）
     * @param id 入库单ID
     */
    public void markAsFinished(Long id) {
        LambdaUpdateWrapper<ReceiptOrder> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(ReceiptOrder::getId, id);
        wrapper.set(ReceiptOrder::getOrderStatus, ServiceConstants.ReceiptOrderStatus.FINISH);
        receiptOrderMapper.update(null, wrapper);
    }

    /**
     * 删除入库单
     */
    public void deleteById(Long id) {
        validateIdBeforeDelete(id);
        receiptOrderMapper.deleteById(id);
    }

    private void validateIdBeforeDelete(Long id) {
        ReceiptOrderVo receiptOrderVo = queryById(id);
        Assert.notNull(receiptOrderVo, "入库单不存在");
        if (ServiceConstants.ReceiptOrderStatus.FINISH.equals(receiptOrderVo.getOrderStatus())) {
            throw new ServiceException("删除失败", HttpStatus.CONFLICT,"入库单【" + receiptOrderVo.getOrderNo() + "】已入库，无法删除！");
        }
    }

    /**
     * 批量删除入库单
     */
    public void deleteByIds(Collection<Long> ids) {
        receiptOrderMapper.deleteBatchIds(ids);
    }

    public void validateReceiptOrderNo(String receiptOrderNo) {
        LambdaQueryWrapper<ReceiptOrder> receiptOrderLqw = Wrappers.lambdaQuery();
        receiptOrderLqw.eq(ReceiptOrder::getOrderNo, receiptOrderNo);
        ReceiptOrder receiptOrder = receiptOrderMapper.selectOne(receiptOrderLqw);
        Assert.isNull(receiptOrder, "入库单号重复，请手动修改");
    }
}
