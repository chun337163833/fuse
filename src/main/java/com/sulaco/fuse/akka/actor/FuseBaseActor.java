package com.sulaco.fuse.akka.actor;

import java.util.Optional;

import com.sulaco.fuse.akka.message.FuseSuspendMessageImpl;
import org.springframework.context.ApplicationContext;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.UntypedActor;

import com.sulaco.fuse.akka.message.FuseInternalMessage;
import com.sulaco.fuse.akka.message.FuseInternalMessageImpl;
import com.sulaco.fuse.akka.message.FuseRequestMessage;
import com.sulaco.fuse.akka.syslog.SystemLogMessage;
import com.sulaco.fuse.akka.syslog.SystemLogMessage.LogLevel;
import com.sulaco.fuse.akka.syslog.SystemLogMessage.LogMessageBuilder;

public abstract class FuseBaseActor extends UntypedActor {

	protected ActorSelection logger;

    protected ActorSelection animator;

	protected ApplicationContext ctx;
	
	public FuseBaseActor() {
		this.logger   = getContext().actorSelection("/user/logger");
        this.animator = getContext().actorSelection("/user/animator");
	}
	
	public FuseBaseActor(ApplicationContext ctx) {
		this();
		this.ctx = ctx;
		
		if (ctx != null) {
			ctx.getAutowireCapableBeanFactory()
			   .autowireBean(this);
		}
	}
	
	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof FuseInternalMessage) {
			onMessage((FuseInternalMessage) message);
		}
		else {
			unhandled(message);
		}
	}
	
	public void onMessage(FuseInternalMessage message) {
		
		Optional<ActorRef> origin = message.popOrigin();
		
		// push message down the chain
		if (origin.isPresent()) {
			origin.get()
				  .tell(
						  message, 
						  self()
				  );
		}
		else {
			// no more actors that could potentially handle this internal message, this
			// would usually be a logic error
			unhandled(message);
		}
	}
	
	public void send(FuseInternalMessage message, String path) {
        send(message, getContext().actorSelection(path));
	}

    public void send(FuseInternalMessage message, ActorSelection selection) {
        message.pushOrigin(self());
        selection.tell(message, self());
    }

    public void bounce(FuseInternalMessage message, ActorSelection selection, String bouncePath) {
        ActorRef bounceTo = getContext().actorFor(bouncePath);
        message.pushOrigin(bounceTo);
        selection.tell(message, bounceTo);
    }

	@Override
	public void unhandled(Object message) {
		super.unhandled(message);
	}
	
	protected FuseInternalMessage newMessage() {
		return newMessage(null);
	}
	
	protected FuseInternalMessage newMessage(FuseRequestMessage request) {
		
		FuseInternalMessage message = new FuseInternalMessageImpl();
		message.getContext().setRequest(request);
		
		return message;
	}
	
	protected void info(String message) {
		
		LogMessageBuilder builder = SystemLogMessage.builder();
		
		SystemLogMessage logmessage 
			= builder.withLevel(LogLevel.INFO)
					 .withMessage(message)
					 .build();
		
		logger.tell(logmessage, getSelf());
	}

    protected void suspend(FuseRequestMessage message) {
        send(
            new FuseSuspendMessageImpl(message),
            animator
        );
    }

    protected void suspend(FuseRequestMessage message, String bouncePath) {
        bounce(
            new FuseSuspendMessageImpl(message),
            animator,
            bouncePath
        );
    }
}
