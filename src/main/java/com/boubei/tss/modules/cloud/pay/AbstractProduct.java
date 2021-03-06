package com.boubei.tss.modules.cloud.pay;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import com.boubei.tss.PX;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.ICommonDao;
import com.boubei.tss.modules.cloud.CloudService;
import com.boubei.tss.modules.cloud.entity.Account;
import com.boubei.tss.modules.cloud.entity.AccountFlow;
import com.boubei.tss.modules.cloud.entity.CloudOrder;
import com.boubei.tss.modules.cloud.entity.ModuleDef;
import com.boubei.tss.modules.param.ParamConfig;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.entity.RoleUser;
import com.boubei.tss.um.entity.SubAuthorize;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.service.IUserService;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MailUtil;
import com.boubei.tss.util.MathUtil;

public abstract class AbstractProduct {

	protected static ICommonDao commonDao = (ICommonDao) Global.getBean("CommonDao");
	protected IUserService userService = (IUserService) Global.getBean("UserService");
	protected CloudService cloudService = (CloudService) Global.getBean("CloudService");

	public CloudOrder co;
	public ModuleDef md;
	public Map<?, ?> trade_map;
	public String payType;
	public String payer;
	
	public User user;
	public Long userId;
	public String userCode;
	
	public static String ignoreMoneyDiffPayer = "admin";

	/**
	 * out_trade_no: 时间戳_coID
	 */
	public static AbstractProduct createBean(String out_trade_no) {
		Long coId = EasyUtils.obj2Long( out_trade_no.split("-")[1] );
		CloudOrder co = (CloudOrder) commonDao.getEntity(CloudOrder.class, coId);
		if( !out_trade_no.equals(co.getOrder_no()) ) {
			return null;
		}
		return createBean(co);
	}

	public static AbstractProduct createBean(CloudOrder co) {
		String productClazz = co.getType();
		AbstractProduct product = (AbstractProduct) BeanUtil.newInstanceByName(productClazz);
		
		Long module_id = co.getModule_id();
		if (module_id != null) {
			product.md = (ModuleDef) commonDao.getEntity(ModuleDef.class, module_id);
		}
		
		product.co = co;
		return product;
	}

	public void beforeOrder() {
		if ( md != null && co != null ) {
			Integer max_account = (Integer) EasyUtils.checkNull(md.getMax_account(), 99999);
			Integer account_num = EasyUtils.obj2Int( co.getAccount_num() );
			if(account_num > max_account) {
				throw new BusinessException("该产品一次只支持购买最多" + md.getMax_account() + "个账号!");
			}
		}
		beforeOrderInit();
	}

	protected  void beforeOrderInit() { }

	/**
	 * 购买付款后初始化实现
	 * 
	 * @param trade_map
	 * @param real_money
	 * @param payer
	 * @param payType
	 */
	public void afterPay(Map<?, ?> trade_map, Double real_money, String payer, String payType) {
		this.trade_map = trade_map;
		this.payType = payType;
		this.payer = payer;

		this.user = (User) commonDao.getEntities(" from User where loginName = ?", co.getCreator()).get(0);
		
		this.userId = user.getId();
		this.userCode = user.getLoginName();

		if (!CloudOrder.NEW.equals(co.getStatus())) {
			throw new BusinessException("订单" + co.getStatus());
		}

		co.setMoney_real(real_money);
		if (real_money < co.getMoney_cal() && !ignoreMoneyDiffPayer.equals(payer)) {
			co.setRemark("订单金额不符");
			co.setStatus(CloudOrder.PART_PAYED);
		} else {
			co.setStatus(CloudOrder.PAYED);
			
			// 购买成功，正式启用用户（if新用户）
			this.user.setDisabled(ParamConstants.FALSE);
			commonDao.update(this.user);
			
			handle();
			init();
			
			String[] receivers = ParamConfig.getAttribute(PX.NOTIFY_AFTER_PAY_LIST, "boubei@163.com").split(",");
			MailUtil.send("用户付款通知", 
								"用户：" + user.getUserName() 
							+ "\n付款：" + co.getMoney_real() + "元" 
							+ "\n产品：" + co.getProduct()
							+ "\n账号：" + co.getCreator()
							+ "\n参数：" + EasyUtils.obj2String(co.getParams()),
					receivers, MailUtil.DEFAULT_MS);
		}

		co.setPay_date(new Date());
		commonDao.update(co);
	}

	protected abstract void handle();

	protected void init() { 
		
	}

	public String getName() {
		return md.getModule();
	}

	protected String toflowType() {
		return AccountFlow.TYPE0;
	}

	protected String toflowRemark() {
		return null;
	}

	protected Account getAccount() {
		return getAccount(userId);
	}

	protected Account getAccount(Long userId) {
		List<?> accounts = commonDao.getEntities(" from Account where belong_user.id = ?", userId);
		if (accounts.size() > 0) {
			return (Account) accounts.get(0);
		}

		Account account = new Account();
		account.setBalance(0D);
		account.setBalance_freeze(0D);
		account.setBelong_user(userService.getUserById(userId));
		account = (Account) commonDao.create(account);

		return account;
	}

	// 普通购买，同时创建一条充值流水和扣款流水
	protected void createFlows(Account account) {
		createIncomeFlow(account);
		createBuyFlow(account);
	}

	// 创建充值流水
	protected void createIncomeFlow(Account account) {
		AccountFlow flow = new AccountFlow(account, this, AccountFlow.TYPE1, null);
		createFlow(account, flow, co.getMoney_real());
	}

	// 创建扣款流水
	protected void createBuyFlow(Account account) {
		AccountFlow flow = new AccountFlow(account, this, this.toflowType(), this.toflowRemark());
		createFlow(account, flow, -co.getMoney_real());
	}

	private void createFlow(Account account, AccountFlow flow, Double deltaMoney) {
		flow.setMoney(deltaMoney);

		Double balance = MathUtil.addDoubles(account.getBalance(), deltaMoney);
		flow.setBalance(balance);
		account.setBalance(balance);

		commonDao.create(flow);
	}

	protected void createSubAuthorize() {
		int account_num = co.getAccount_num();
		int mouth_num = co.getMonth_num();
		for (int i = 0; i < account_num; i++) {
			SubAuthorize sa = new SubAuthorize();
			sa.setName(md.getId() + "_" + md.getModule() + "_" + userId + "_" + (i + 1)); // name=模块ID_模块名称_购买人_购买序号
			sa.setOwnerId(userId);
			sa.setBuyerId(userId);

			Calendar calendar = new GregorianCalendar();
			calendar.add(Calendar.MONTH, mouth_num);
			sa.setEndDate(calendar.getTime());
			sa.setStartDate(new Date());

			commonDao.create(sa);

			// 创建策略角色对应关系
			List<Long> roleIds = md.roles();
			for (Long roleId : roleIds) {
				RoleUser ru = new RoleUser();
				ru.setModuleId(md.getId());
				ru.setRoleId(roleId);
				ru.setStrategyId(sa.getId());
				ru.setUserId(userId);
				commonDao.create(ru);
			}

		}
	}

}
