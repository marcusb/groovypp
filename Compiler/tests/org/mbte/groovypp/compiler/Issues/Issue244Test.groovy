/*
 * Copyright 2009-2010 MBTE Sweden AB.
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
package org.mbte.groovypp.compiler.Issues

@Typed
class Issue244Test extends GroovyShellTestCase {

    void testMe()
    {
        shell.evaluate """
package p

@Typed
public BigDecimal payment(BigDecimal amount,BigDecimal rate, int p)
{
   BigDecimal tax = 0.13
   BigDecimal taxedRate = (1+tax) * rate
   def m = ((1+taxedRate)**p)
   println "\${m.class} \$m"
   def n = taxedRate * m
   println "\${n.class} \$n"
   BigDecimal payment = n;  //i(1+i)^n
   def k = ((((1+taxedRate)**p))-1)
   println "\${k.class} \$k"
   def l = payment / k
   println "\${l.class} \$l"
   payment = l;
   println "\$l \$payment"
   payment * amount
}

public BigDecimal paymentUntyped(BigDecimal amount,BigDecimal rate, int p)
{
   BigDecimal tax = 0.13
   BigDecimal taxedRate = (1+tax) * rate
   def m = ((1+taxedRate)**p)
   println "\${m.class} \$m"
   def n = taxedRate * m
   println "\${n.class} \$n"
   BigDecimal payment = n;  //i(1+i)^n
   def k = ((((1+taxedRate)**p))-1)
   println "\${k.class} \$k"
   def l = payment / k
   println "\${l.class} \$l"
   payment = l
   println "\$l \$payment"
   payment * amount
}
def res = paymentUntyped(new BigDecimal("300"),new BigDecimal("0.045"),12).toString()
println res
assert res == '34.01007900970739461499192657356616109609603881835937500'

println ""

res = payment(new BigDecimal("300"),new BigDecimal("0.045"),12).toString()
println res
assert res == '34.01007900970739461499192657356616109609603881835937500'
"""
    }
}