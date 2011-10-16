/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.messaging.remote.internal

import org.gradle.api.Action
import org.gradle.messaging.concurrent.DefaultExecutorFactory
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import static org.gradle.util.TextUtil.*

import spock.lang.*
import spock.util.concurrent.BlockingVariable

class InputForwarderTest extends Specification {

    def bufferSize = 1024
    def executerFactory = new DefaultExecutorFactory()

    def source = new PipedOutputStream()
    def inputStream = new PipedInputStream(source)

    def received = new LinkedBlockingQueue()
    def finishedHolder = new BlockingVariable(2)

    def action = action { received << it }
    def onFinish = finished { finishedHolder.set(true) }

    def action(Closure action) {
        this.action = action as Action
    }

    def finished(Closure runnable) {
        this.onFinish = runnable
    }

    def forwarder

    def createForwarder() {
        forwarder = new InputForwarder(inputStream, action, onFinish, executerFactory, bufferSize)
        forwarder.start()
    }

    def receive(receive) {
        assert received.poll(5, TimeUnit.SECONDS) == receive
        true
    }

    boolean isFinished() {
        finishedHolder.get() == true
    }

    boolean isNoMoreInput() {
        receive null
    }

    def setup() {
        createForwarder()
    }

    def closeInput() {
        inputStream.close()
        source.close()
    }

    def waitForForwarderToCollect() {
        sleep 1000
    }

    def "input from source is forwarded until forwarder is stopped"() {
        when:
        source << toPlatformLineSeparators("abc\ndef\njkl")
        waitForForwarderToCollect()
        forwarder.stop()

        then:
        receive toPlatformLineSeparators("abc\n")
        receive toPlatformLineSeparators("def\n")
        receive "jkl"
        noMoreInput

        and:
        finished
    }

    def "input from source is forwarded until source input stream is closed"() {
        when:
        source << toPlatformLineSeparators("abc\ndef\njkl")
        waitForForwarderToCollect()
        closeInput()

        then:
        receive toPlatformLineSeparators("abc\n")
        receive toPlatformLineSeparators("def\n")
        receive "jkl"
        noMoreInput

        and:
        finished
    }

    def "output is buffered by line"() {
        when:
        source << "a"

        then:
        noMoreInput

        when:
        source << "b"

        then:
        noMoreInput

        when:
        source << toPlatformLineSeparators("\n")

        then:
        receive toPlatformLineSeparators("ab\n")
    }

    def "one partial line when input stream closed gets forwarded"() {
        when:
        source << "abc"
        waitForForwarderToCollect()

        and:
        closeInput()

        then:
        receive "abc"

        and:
        noMoreInput
    }

    def "one partial line when forwarder stopped gets forwarded"() {
        when:
        source << "abc"
        waitForForwarderToCollect()

        and:
        forwarder.stop()

        then:
        receive "abc"

        and:
        noMoreInput
    }

    def "forwarder can be closed before receiving any output"() {
        when:
        forwarder.stop()

        then:
        noMoreInput
    }

    def "can handle lines larger than the buffer size"() {
        given:
        def longLine = toPlatformLineSeparators("a" * (bufferSize * 10) + "\n")

        when:
        source << longLine << longLine

        then:
        receive longLine
        receive longLine
        noMoreInput
    }

    def cleanup() {
        closeInput()
        forwarder.stop()
    }

}