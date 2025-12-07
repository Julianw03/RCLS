package com.julianw03.rcls.model.services;

public interface DiscriminatedSerializable<I extends Enum<I> & DiscriminatorEnum> extends ServiceSerializable {
    I getDiscriminator();
}
