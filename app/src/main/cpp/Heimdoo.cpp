//
// Created by Rohit Verma on 02-05-2023.
//

#include "Heimdoo.h"

#include <Heimdall.h>
#include <HelpAction.h>
#include <Interface.h>
#include <BridgeManager.h>
#include <libpit.h>

using namespace std;
using namespace Heimdall;

namespace heimdoo {
    int exec_heimdall(int fd, int argc, char *argv[]) {
        if (argc < 2) {
            Interface::PrintUsage();
            return 0;
        }
        int result = 0;
        auto actionIt = Interface::GetActionMap().find(
                argv[1]);
        if (actionIt != Interface::GetActionMap().end())
            result = actionIt->second.executeFunction(fd, argc, argv);
        else
            result = HelpAction::Execute(fd, argc, argv);
        return result;
    }

    bool is_heimdall_device(int fd) {
        auto *bridgeManager = new BridgeManager(false);
        bridgeManager->SetUsbLogLevel(BridgeManager::UsbLogLevel::None);
        bool detected = bridgeManager->DetectDevice(fd);
        delete bridgeManager;
        return detected;
    }
} // heimdoo

