package com.sulaco.fuse.akka.message;


public interface FuseInternalMessage extends FuseOriginChain {
	
	FuseMessageContext getContext();

    long getTimestamps();

    FuseInternalMessage timestamp();
}
