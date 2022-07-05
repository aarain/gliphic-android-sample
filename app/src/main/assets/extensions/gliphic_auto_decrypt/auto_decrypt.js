"use strict";

/*
 * The regular expression matching the start and end tags of an encrypted message.
 * This regular expression assumes that the start tag is "|~" and the end tag is "~|" and uses a global matcher.
 */
const regex_to_match_encrypted_messages = /\|~.+?~\|/g;     // TODO: Reference the start and end tags from GlobalUtils.

// Establish a connection with the background script.
let portToBS = browser.runtime.connect({name:"port-from-cs"});

portToBS.onMessage.addListener(function(jo) {
    loop_nodes(replace_messages, jo.allMsgPairs);
});

// Identify encrypted messages and send them to the background script for decryption.
var encrypted_messages = loop_nodes(identify_messages, null);
if (encrypted_messages.length != 0) {
    portToBS.postMessage({ct:encrypted_messages});
}

function construct_regex(to_match) {
    // Automatically escape special characters.
    return new RegExp(to_match.replace(/[-\/\\^$*+?.()|[\]{}]/g, "\\$&"), "g");
}

/*
 * Loop through every child node in every element in the document and execute the given function on each node.
 */
function loop_nodes(funct, all_message_pairs) {
    var encrypted_messages = [];

    var elements = document.getElementsByTagName("*");

    for (var i = 0; i < elements.length; i++) {
        var element = elements[i];

        for (var j = 0; j < element.childNodes.length; j++) {
            var child_node = element.childNodes[j];

            // Ignore HTML source comments.
            if (child_node.nodeType == 8) continue;

            // Handle image sources and text differently.
            if (child_node instanceof HTMLImageElement) {
                var original_text = child_node.src;
            }
            else {
                var original_text = child_node.nodeValue;
            }

            if (original_text === null) continue;

            // Execute the given function.
            if (all_message_pairs === null) {
                // Modify the array of identified encrypted messages found.
                encrypted_messages = funct(original_text, encrypted_messages);
            }
            else {
                // There are no encrypted messages to return since they have already been decrypted.
                // The calling method should ignore the return value of this method.
                funct(element, child_node, original_text, all_message_pairs);
            }
        }
    }

    return encrypted_messages;
}

/*
 * Identify every encrypted message and return them as an array.
 */
function identify_messages(original_text, encrypted_messages) {
    // Array of all encrypted messages found in the current node.
    var message_matches = original_text.match(regex_to_match_encrypted_messages);

    // If the search pattern is not found anywhere in the node, return immediately to skip to searching the next node.
    if (message_matches !== null) {
        // Add elements from the matched messages array to the return array.
        for (var i = 0; i < message_matches.length; i++) {
            encrypted_messages.push(message_matches[i]);
        }
    }

    return encrypted_messages;
}

/*
 * Use the given array of encrypted and decrypted messages to identify and replace messages in the document.
 */
function replace_messages(element, child_node, original_text, all_message_pairs) {
    var replaced_text = original_text;

    for (var i = 0; i < all_message_pairs.length; i++) {
        var cipher_text = all_message_pairs[i].ct;
        var plain_text  = all_message_pairs[i].pt;

        // Regular expression matching the specific cipher text.
        var regex_to_match = construct_regex(cipher_text);

        // Array of all encrypted messages found in the current node.
        var msg_matches = original_text.match(regex_to_match);

        // If the search pattern is not found anywhere in the node, skip to searching for the next encrypted message.
        if (msg_matches === null) continue;

        replaced_text = replaced_text.replace(cipher_text, plain_text);
    }

    // Replace the displayed content regardless of how many encrypted messages have been found.
    if (child_node instanceof HTMLImageElement) {
        child_node.src = replaced_text;
    }
    else {
        element.replaceChild(document.createTextNode(replaced_text), child_node);
    }
}