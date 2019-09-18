package org.tron.walletcli.task;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.api.GrpcAPI.AddressPrKeyPairMessage;
import org.tron.sunapi.ErrorCodeEnum;
import org.tron.sunapi.SunNetwork;
import org.tron.sunapi.SunNetworkResponse;
import org.tron.sunapi.response.TransactionResponse;
import org.tron.walletcli.config.ConfigInfo;

public class AccountTask extends SideChainTask {

  private static final Logger logger = LoggerFactory.getLogger("AccountTask");

  private List<String> accountList = new ArrayList<>();

  public AccountTask () {
    File file = new File(ConfigInfo.privateKeyAddressFile);
    if (file.exists()) {
      BufferedReader reader = null;
      try {
        reader = new BufferedReader(new FileReader(file));
        String line = null;
        while ((line = reader.readLine()) != null) {
          accountList.add(line);
        }
        reader.close();

      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }


  public void runTask(SunNetwork sdk) {
    sendCoinAndDeposit(sdk);
  }

  public void sendCoinAndDeposit(SunNetwork sdk) {

    logger.info("send coin and deposit");

    while (true) {
      sdk.setPrivateKey(ConfigInfo.privateKey);
      accountList.forEach(account -> {
        SunNetworkResponse<TransactionResponse> sunNetworkResponse = sdk.getSideChainService()
            .sendCoin(account.split(",")[0], ConfigInfo.contractDepositValue);
        logger.info("sendcoin txid = {}", sunNetworkResponse.getData().getTrxId());
        try {
          Thread.sleep(ConfigInfo.interval);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      });
      accountList.forEach(account -> {
        sdk.setPrivateKey(account.split(",")[1]);
        triggerContract(sdk, ConfigInfo.contractDepositValue, ConfigInfo.contractDeposit);
        try {
          Thread.sleep(ConfigInfo.interval);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      });
    }
  }

  public void initAccounts (SunNetwork sdk) {

    logger.info("init accounts !");
    sdk.setPrivateKey(ConfigInfo.privateKey);
    if (accountList.size() > 0) {
      accountList.forEach(account -> {
//        SunNetworkResponse<TransactionResponse> sunNetworkResponse = sdk.getSideChainService()
//            .freezeBalance(ConfigInfo.accountFreezeBalance, 3, 1, account.split(",")[0]);
//        logger.info("freeze txid = {}", sunNetworkResponse.getData().getTrxId());
      });
    } else {
      BufferedWriter out = null;
      try {
        out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(ConfigInfo.privateKeyAddressFile, true)));
        for (int i = 0; i < ConfigInfo.accountNum; i ++ ) {

          sdk.setPrivateKey(ConfigInfo.privateKey);
          SunNetworkResponse<AddressPrKeyPairMessage> resp = sdk.getMainChainService().generateAddress();
          if (resp.getCode() == ErrorCodeEnum.SUCCESS.getCode()) {
            String info = resp.getData().getAddress() + "," + resp.getData().getPrivateKey();
            sdk.getSideChainService().createAccount(resp.getData().getAddress());
//            SunNetworkResponse<TransactionResponse> sunNetworkResponse = sdk.getSideChainService()
//                .freezeBalance(ConfigInfo.accountFreezeBalance, 3, 1, resp.getData().getAddress());
//            logger.info("freeze txid = {}", sunNetworkResponse.getData().getTrxId());
            accountList.add(info);
            out.write(info);
            out.newLine();
            out.flush();
            try {
              Thread.sleep(10);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
        out.close();
      } catch (Exception e) {

      }
    }
  }

}
