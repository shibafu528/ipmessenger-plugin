package org.jenkinsci.plugins.IPMessengerSendStep
f = namespace(lib.FormTagLib)
f.entry(field: 'message', title: 'Message') {
    f.textarea(value: instance == null ? '$BUILD_URL' : instance.message)
}
f.entry(field: 'recipientHosts', title: 'Recipient host list') {
    f.textarea(value: instance == null ? '' : instance.recipientHosts.join('\n'))
}