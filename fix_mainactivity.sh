#!/bin/bash
# 修复 MainActivity 中错误的 delegate 方法引用

SRC="app/src/main/java/com/course/imchat/MainActivity.kt"

# 1. ChatViewModelFactory -> 直接删除那两行，然后用老的 viewModel 方式
sed -i 's/val messageCache.*//; s/val sessionCache.*//; s/val factory.*//' "$SRC"

# 2. 修复 delegate 方法名
sed -i 's/viewModel.message::doQuote/viewModel::quoteMessage/' "$SRC"
sed -i 's/viewModel.message.cancelQuote/viewModel.message.cancelQuote/' "$SRC"  
sed -i 's/viewModel.message::doEdit/viewModel.message::onEditMessage/' "$SRC"
sed -i 's/viewModel.message.cancelEdit/viewModel.message.cancelEditMessage/' "$SRC"
sed -i 's/viewModel.message::saveEdit/viewModel.message::saveEditMessage/' "$SRC"
sed -i 's/viewModel.message::doForward/viewModel.message::onForwardMessage/' "$SRC"
sed -i 's/viewModel.message::deleteSelectedMessages/viewModel.message::deleteSelected/' "$SRC"
sed -i 's/viewModel.message::forwardSelectedMessages/viewModel.message::forwardSelected/' "$SRC"
sed -i 's/viewModel.message::exitMultiSelect/viewModel.message::cancelMultiSelect/' "$SRC"
sed -i 's/viewModel.message::stopVoice/viewModel.message::stopVoiceRecording/' "$SRC"
sed -i 's/viewModel.group::setCreateGroupName/viewModel.group::onCreateGroupNameChange/' "$SRC"
sed -i 's/viewModel.group::confirmCreateGroup/viewModel.group::confirmCreateGroup/' "$SRC"
sed -i 's/viewModel.pin::saveMessageToCollection/viewModel.pin::saveMessageToCollection/' "$SRC"
sed -i 's/viewModel.pin::unpinCurrent/viewModel.pin::unpinCurrent/' "$SRC"
sed -i 's/viewModel.group::handleCreateGroup/viewModel.group::handleCreateGroup/' "$SRC"

echo "fixed MainActivity"
