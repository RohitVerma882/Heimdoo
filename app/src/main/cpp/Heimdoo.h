//
// Created by Rohit Verma on 02-05-2023.
//

#ifndef HEIMDOO_HEIMDOO_H
#define HEIMDOO_HEIMDOO_H

namespace heimdoo {

    int exec_heimdall(int fd, int argc, char *argv[]);

    bool is_heimdall_device(int fd);

} // heimdoo

#endif //HEIMDOO_HEIMDOO_H
