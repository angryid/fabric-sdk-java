/*
Copyright IBM Corp. 2016 All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

		 http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package main

import (
	"fmt"

	"github.com/hyperledger/fabric/core/chaincode/shim"
	pb "github.com/hyperledger/fabric/protos/peer"
	"bytes"
	"time"
	"strconv"
)

var logger = shim.NewLogger("ule_cc0")

// SimpleChaincode example simple Chaincode implementation
type SimpleChaincode struct {
}

// Init initializes the chaincode state
func (t *SimpleChaincode) Init(stub shim.ChaincodeStubInterface) pb.Response {
	logger.Info("########### ule_cc Init ###########")
	var err error
	//导入已存在合约，如果有
	_, args := stub.GetFunctionAndParameters()
	//判断已存在合约是否为空
	if args == nil {
		return shim.Success([]byte("Empty ule_cc init success"))
	}
	//判断合约参数是否偶数 奇数=商户id，偶数=商户合约内容
	if len(args)%2 != 0 {
		return shim.Error("无效参数")
	}
	//把合约添加进账本
	for tempIndex := 0; tempIndex < len(args); tempIndex = tempIndex + 2 {
		err = stub.PutState(args[tempIndex], []byte(args[tempIndex+1]))
		if err != nil {
			return shim.Error(err.Error())
		}
	}
	//获取批量事务状态
	if transientMap, err := stub.GetTransient(); err == nil {
		if transientData, ok := transientMap["result"]; ok {
			return shim.Success(transientData)
		}
	}
	return shim.Success([]byte("Batch ule_cc init success"))

}


// 执行方法
func (t *SimpleChaincode) Invoke(stub shim.ChaincodeStubInterface) pb.Response {
	logger.Info("########### ule_cc Invoke ###########")
	function, args := stub.GetFunctionAndParameters()
	//添加或更新合约
	if function == "addOrUpdate" {
		return t.addOrUpdate(stub, args)
	}
	//查询合约
	if function == "query" {
		return t.query(stub, args)
	}
	//查询交易历史
	if function == "queryHistory" {
		return t.queryHistory(stub, args)
	}
	logger.Errorf("未知的方法名，目前只支持addOrUpdate或query,queryHistory. 但是收到的方法名为: %v", args[0])
	return shim.Error(fmt.Sprintf("未知的方法名，目前只支持addOrUpdate或query,queryHistory. 但是收到的方法名为: %v", args[0]))
}

//添加或更新合约
func (t *SimpleChaincode) addOrUpdate(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	var err error
	if len(args) != 2 {
		return shim.Error("参数无效")
	}
	// 写入账本
	err = stub.PutState(args[0], []byte(args[1]))
	if err != nil {
		return shim.Error(err.Error())
	}
	if transientMap, err := stub.GetTransient(); err == nil {
		if transientData, ok := transientMap["event"]; ok {
			stub.SetEvent("event", transientData)
		}
		if transientData, ok := transientMap["result"]; ok {
			return shim.Success(transientData)
		}
	}
	return shim.Success([]byte("AddOrUpdate contract Success merchantId=" + args[0]))
}

// 查询合约
func (t *SimpleChaincode) query(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	var merchantId string // Entities
	var err error
	if len(args) != 1 {
		return shim.Error("无效的参数")
	}
	merchantId = args[0]
	// 从账本获取
	contractByte, err := stub.GetState(merchantId)
	if err != nil {
		return shim.Error(fmt.Sprintf("获取账本异常，merchantId:%s，error:%s", merchantId, err))
	}
	if contractByte == nil {
		return shim.Error(fmt.Sprintf("获取合约为空，merchantId:%s", merchantId))
	}
	return shim.Success(contractByte)
}

// 查询历史合约
func (t *SimpleChaincode) queryHistory(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	var merchantId string // Entities
	var err error
	var result bytes.Buffer

	if len(args) != 1 {
		return shim.Error("无效的参数")
	}
	merchantId = args[0]
	// 从账本获取 HistoryQueryIteratorInterface
	contractHistoryIterator, err := stub.GetHistoryForKey(merchantId)
	if err != nil {
		return shim.Error(fmt.Sprintf("获取账本历史数据异常，merchantId:%s，error:%s", merchantId, err))
	}
	if contractHistoryIterator == nil || !contractHistoryIterator.HasNext() {
		return shim.Error(fmt.Sprintf("获取账本历史数据为空，merchantId:%s", merchantId))
	}

	result.WriteString("[")
	for it := contractHistoryIterator; it.HasNext(); {
		history, err := it.Next()
		if err != nil {
			result.WriteString("{\"txId\":")
			result.WriteString("\"")
			result.WriteString(history.TxId)
			result.WriteString("\"")
			result.WriteString(", \"value\":")
			result.WriteString("\"")
			result.WriteString(string(history.GetValue()))
			result.WriteString("\"")
			result.WriteString(", \"timestamp\":")
			result.WriteString("\"")
			result.WriteString(time.Unix(history.Timestamp.Seconds, int64(history.Timestamp.Nanos)).String())
			result.WriteString("\"")
			result.WriteString(", \"isDelete\":")
			result.WriteString("\"")
			result.WriteString(strconv.FormatBool(history.IsDelete))
			result.WriteString("\"}")
		}
	}
	result.WriteString("]")

	return shim.Success(result.Bytes())
}

func main() {
	err := shim.Start(new(SimpleChaincode))
	if err != nil {
		logger.Errorf("Error starting Simple chaincode: %s", err)
	}
}
